package project.homelearn.service.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import project.homelearn.dto.common.RecaptchaResponse;

@Slf4j
@Service
public class RecaptchaService {

    @Value("${recaptcha.key}")
    private String secretKey;

    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    public boolean verifyRecaptcha(String token) {
        log.info("token = {}", token);
        RestTemplate restTemplate = new RestTemplate();

        // 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 바디 설정
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("secret", secretKey);
        params.add("response", token);

        // 헤더와 바디를 포함한 HttpEntity 생성
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

        ResponseEntity<RecaptchaResponse> responseEntity = restTemplate.postForEntity(
                RECAPTCHA_VERIFY_URL,
                requestEntity,
                RecaptchaResponse.class
        );

        RecaptchaResponse response = responseEntity.getBody();
        log.info("reCAPTCHA response = {}", response);
        return response != null && response.isSuccess();
    }
}