package com.springles.game;

import com.springles.domain.constants.GameRole;
import com.springles.domain.entity.ChatRoom;
import com.springles.domain.entity.GameSession;
import com.springles.domain.entity.Player;
import com.springles.exception.CustomException;
import com.springles.exception.constants.ErrorCode;
import com.springles.repository.GameSessionRedisRepository;
import com.springles.repository.PlayerRedisRepository;
import groovy.util.logging.Slf4j;
import jakarta.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class GameSessionManager {

    private final GameSessionRedisRepository gameSessionRedisRepository;
    private final PlayerRedisRepository playerRedisRepository;
    private final RoleManager roleManager;

    /* 게임 세션 생성 */
    public void createGame(ChatRoom chatRoom) {
        GameSession gameSession = gameSessionRedisRepository.save(GameSession.of(chatRoom));
        addUser(chatRoom.getId(), chatRoom.getOwnerId());
    }

    /* 게임 시작 */
    public void startGame(Long roomId) {
        List<Player> players = findPlayersByRoomId(roomId);
        if (players.size() < 5 || players.size() > 10) {
            throw new CustomException(ErrorCode.PLAYER_HEAD_ERROR);
        }
        roleManager.assignRole(players);
        GameSession gameSession = findGameByRoomId(roomId);
        gameSessionRedisRepository.save(gameSession.start(players.size()));
        // 메세지 보내기
    }

    /* 게임 종료 -> 준비 상태로 돌아가기 */
    public void endGame(Long roomId) {
        GameSession gameSession = findGameByRoomId(roomId);
        gameSession.end();
        gameSessionRedisRepository.save(gameSession);
        List<Player> players = findPlayersByRoomId(roomId);
        for (Player player : players) {
            player.updateRole(GameRole.NONE);
        }
        playerRedisRepository.saveAll(players);

    }

    /* 게임 세션 삭제 */
    public void removeGame(Long roomId) {
        List<Player> players = findPlayersByRoomId(roomId);
        if (!players.isEmpty()) {
            throw new CustomException(ErrorCode.GAME_PLAYER_EXISTS);
        }
        gameSessionRedisRepository.deleteById(roomId);
    }

    /* 게임에서 유저 제거 */
    public void removePlayer(Long roomId, Long playerId) {
        Player player = findPlayerByMemeberId(playerId);
        GameSession gameSession = findGameByRoomId(roomId);
        playerRedisRepository.deleteById(playerId);
        List<Player> players = findPlayersByRoomId(roomId);
        // 아무도 없다면 방삭제
        if (players.isEmpty()) {
            removeGame(roomId);
        }
        // 남은 플레이어가 존재하고 방장이 나갔다면 랜덤으로 방장 넘겨주기
        else if (Objects.equals(gameSession.getHostId(), playerId)) {
            Random random = new Random();
            gameSession.changeHost(players.get(random.nextInt(players.size())).getMemberId());
            gameSessionRedisRepository.save(gameSession);
        }
    }

    /* 게임에 유저 추가 */
    public void addUser(Long roomId, Long memberId) {
        if (playerRedisRepository.findByRoomId(roomId).size() > 10) {
            throw new CustomException(ErrorCode.GAME_HEAD_FULL);
        }
        GameSession gameSession = findGameByRoomId(roomId);
        // 아직 다른 방에 참가중이라면 -> 게임 중간에 나갔을 경우 발생 가능 -> 중간에 나갔을 경우를 처리해야 함.
        if (playerRedisRepository.existsByMemberId(memberId)) {
            throw new CustomException(ErrorCode.PLAYER_STILL_INGAME);
        }
        playerRedisRepository.save(Player.of(memberId, roomId));
    }

    public GameSession findGameByRoomId(Long roomId) {
        return gameSessionRedisRepository.findById(roomId)
            .orElseThrow(() -> new CustomException(ErrorCode.GAME_NOT_FOUND));
    }

    public GameSession findGamByHostId(Long hostId) {
        return gameSessionRedisRepository.findByHostId(hostId)
            .orElseThrow(() -> new CustomException(ErrorCode.GAME_NOT_FOUND));
    }

    public List<Player> findPlayersByRoomId(Long roomId) {
        return playerRedisRepository.findByRoomId(roomId);
    }

    public Player findPlayerByMemeberId(Long memberId) {
        return playerRedisRepository.findById(memberId)
            .orElseThrow(() -> new CustomException(ErrorCode.PLAYER_NOT_FOUND));
    }

}