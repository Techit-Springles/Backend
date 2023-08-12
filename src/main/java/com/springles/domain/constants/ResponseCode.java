package com.springles.domain.constants;

import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

@Getter
@ToString
public enum ResponseCode {

    /* CHATROOM */
    CHATROOM_CREATE(HttpStatus.CREATED, "201", "채팅방 생성 성공"),

    /* AUTH */
    MEMBER_SAVE(HttpStatus.CREATED, "201", "회원가입 성공"),
    MEMBER_LOGIN(HttpStatus.OK, "200", "로그인 성공"),
    MEMBER_LOGOUT(HttpStatus.NO_CONTENT, "204", "로그아웃 성공"),

    /* MEMBER */
    MEMBER_DETAIL(HttpStatus.OK, "200", "회원정보 불러오기 성공"),
    MEMBER_UPDATE(HttpStatus.OK, "200", "회원정보 수정 성공"),
    MEMBER_DELETE(HttpStatus.NO_CONTENT, "204", "회원정보 삭제 성공"),
    MEMBER_EXISTS(HttpStatus.OK,"200","회원존재 여부 조회 성공");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ResponseCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

}