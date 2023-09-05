package com.springles.controller.message;

import com.springles.domain.constants.GamePhase;
import com.springles.domain.constants.GameRole;
import com.springles.domain.constants.ResponseCode;
import com.springles.domain.dto.chatroom.ChatRoomResponseDto;
import com.springles.domain.entity.ChatRoom;
import com.springles.domain.entity.GameSession;
import com.springles.domain.entity.Player;
import com.springles.game.ChatMessage;
import com.springles.game.GameSessionManager;
import com.springles.game.MessageManager;
import com.springles.service.ChatRoomService;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@RequiredArgsConstructor
@Controller
public class MessageController {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final GameSessionManager gameSessionManager;
    private final MessageManager messageManager;
    private final ChatRoomService chatRoomService;

    /*메세지 전송*/
    @MessageMapping("/chat/{roomId}")
    public void sendMessage(SimpMessageHeaderAccessor accessor, String message,
        @DestinationVariable Long roomId) {

        log.info("수신 메시지: "+message+ " 방 id: "+roomId +" 발송자: "+getMemberName(accessor));

        GameSession gameSession = gameSessionManager.findGameByRoomId(roomId);
        Player player = gameSessionManager.findPlayerByMemberName(getMemberName(accessor));
        // 관전자는 관전자들끼리만 채팅이 가능
        if (player.getRole().equals(GameRole.OBSERVER)) {
            messageManager.sendMessage("/sub/chat/" + roomId + "/" + "observer", message, roomId,
                player.getMemberName());
            return;
        }
        // 밤 투표시간에는 마피아끼리만 채팅 가능
        if (gameSession.getGamePhase().equals(GamePhase.NIGHT_VOTE)) {
            if (player.getRole().equals(GameRole.MAFIA)) {
                messageManager.sendMessage("/sub/chat/" + roomId + "/" + "mafia", message, roomId,
                    player.getMemberName());
            }
            return;
        }
        // 위의 모든 조건이 아니라면 방에 참여한 모두에게 메시지 전송
        messageManager.sendMessage("/sub/chat/" + roomId, message, roomId,
            player.getMemberName());
    }

    /*게임 생성*/
    @MessageMapping("/gameCreate/{roomId}")
    public void sendMessage_GameCreate(@DestinationVariable Long roomId) {
        log.info("게임 생성");
        gameSessionManager.createGame(roomId);
    }

    /*게임 참여*/
    @MessageMapping("/gameJoin/{roomId}")
    public void sendMessage_GameJoin(SimpMessageHeaderAccessor accessor,
        @DestinationVariable Long roomId) {
        String memberName = getMemberName(accessor);

        messageManager.sendMessage(
            "/sub/chat/" + roomId + "/" + "playerList",
            gameSessionManager.addUser(roomId, memberName));
        // 게임 참여 메시지 전송
        messageManager.sendMessage(
            "/sub/chat/" + roomId,
            memberName + "님이 입장하셨습니다.",
            roomId, "admin");
    }

    /*게임 나가기*/
    @MessageMapping("/gameExit/{roomId}")
    public void sendMessage_GameExit(SimpMessageHeaderAccessor accessor,
        @DestinationVariable Long roomId) {
        String memberName = getMemberName(accessor);
        gameSessionManager.removePlayer(roomId, memberName);
        messageManager.sendMessage(
            "/sub/chat/" + roomId + "/" + "playerList",
            gameSessionManager.findPlayersByRoomId(roomId)
        );
        messageManager.sendMessage(
            "/sub/chat/" + roomId,
            memberName + "님이 퇴장하셨습니다.",
            roomId, "admin"
        );
    }

    /*게임 시작*/
    @MessageMapping("/gameStart/{roomId}")
    public void sendMessage_GameStart(SimpMessageHeaderAccessor accessor,
        @DestinationVariable Long roomId) {

        messageManager.sendMessage("/sub/chat/" + roomId,
            "게임이 시작되었습니다.",
            roomId, "admin");

        List<Player> mafiaList = new ArrayList<>();
        gameSessionManager.startGame(roomId, getMemberName(accessor)).forEach(p -> {
            messageManager.sendMessage(
                "/sub/chat/" + roomId + "/" + p.getMemberId(),
                "당신은 " + p.getRole() + "입니다.",
                roomId, "admin"
            );
            if (p.getRole().equals(GameRole.MAFIA)) {
                mafiaList.add(p);
            }
        });

        String mafiaListString = mafiaList.stream()
            .map(Player::getMemberName)
            .collect(Collectors.joining(", "));

        mafiaList.forEach(m -> {
            messageManager.sendMessage(
                "/sub/chat/" + roomId + "/" + m.getMemberId(),
                "마피아 플레이어는" + " [" + mafiaListString + "] " + "입니다.",
                roomId, "admin"
            );
        });
    }

    /*게임 정보 수정?*/
    @MessageMapping("/gameUpdate/{roomId}")
    public void sendMessage_GameUpdate(SimpMessageHeaderAccessor accessor,
        @DestinationVariable Long roomId) {
        messageManager.sendMessage(
            "/sub/chat/" + roomId,
            "게임 정보가 변경되었습니다.",
            roomId, "admin"
        );
    }

    public String getMemberName(SimpMessageHeaderAccessor accessor) {
        return accessor.getUser().getName().split(",")[1].split(":")[1].trim();
    }
}
