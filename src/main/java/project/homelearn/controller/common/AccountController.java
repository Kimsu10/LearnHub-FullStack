package project.homelearn.controller.common;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.homelearn.dto.common.account.EmailCodeDto;
import project.homelearn.dto.common.account.EmailDto;
import project.homelearn.dto.common.account.PasswordResetDto;
import project.homelearn.service.common.RecaptchaService;
import project.homelearn.service.common.UserService;

/**
 * Author : 정성진
 */
@Slf4j
@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;
    private final RecaptchaService recaptchaService;

    // 아이디 찾기
    @PostMapping("/find-id")
    public ResponseEntity<?> findId(@Valid @RequestBody EmailDto emailDto) {
        String result = userService.findIdService(emailDto);
        if (result != null) {
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
        // 아이디를 못 찾은 경우
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    // 비밀번호 재설정 전 이메일로 코드 전송
    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@Valid @RequestBody EmailDto emailDto) {
        boolean result = userService.sendCodeForResetPassword(emailDto);
        if (result) {
            return new ResponseEntity<>(HttpStatus.OK);
        }
        // 이메일로 가입된 정보가 없을 경우
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    // 비밀번호 재설정 전 코드 인증
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@Valid @RequestBody EmailCodeDto emailCodeDto) {
        String username = userService.verifyCodeForResetPassword(emailCodeDto);
        if (username != null) {
            // 코드가 유효한 경우 아이디 응답
            return new ResponseEntity<>(username, HttpStatus.OK);
        }
        // 코드가 일치하지 않은 경우
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    // 비밀번호 재설정
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetDto passwordResetDto) {
        boolean isHuman = recaptchaService.verifyRecaptcha(passwordResetDto.getRecaptcha());
        if (!isHuman) {
            return new ResponseEntity<>("봇입니다.", HttpStatus.BAD_REQUEST);
        }

        boolean result = userService.resetPassword(passwordResetDto);
        if (result) {
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
}