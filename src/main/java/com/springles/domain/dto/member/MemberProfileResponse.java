package com.springles.domain.dto.member;

import com.springles.domain.entity.MemberGameInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class MemberProfileResponse {

    private String nickname;    // 게임 닉네임
    private Enum profileImg;
    private Long level; // 유저 레벨
    private Long exp;   // 유저 경험치
    private String inGameRole;  // 게임 내 직업
    private Long memberId;

    public static MemberProfileResponse of(MemberGameInfo memberGameInfo, Long memberId) {
        return MemberProfileResponse.builder()
                .memberId(memberId)
                .nickname(memberGameInfo.getNickname())
                .profileImg(memberGameInfo.getProfileImg())
                .level(memberGameInfo.getLevel())
                .exp(memberGameInfo.getExp())
                .inGameRole(memberGameInfo.getInGameRole())
                .build();
    }
}