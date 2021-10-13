package kr.carrot.springsecurity.app.controller;

import kr.carrot.springsecurity.annotation.QueryParams;
import kr.carrot.springsecurity.app.dto.LoginDto;
import kr.carrot.springsecurity.app.dto.request.AuthorizationRequestDto;
import kr.carrot.springsecurity.app.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final ClientService clientService;

    @GetMapping("/oauth2/auth")
    public String loginForm(@QueryParams AuthorizationRequestDto requestDto, Model model) {

        if (clientService.isCorrectAuthParam(requestDto)) {
            model.addAttribute("loginDto", new LoginDto());
            return "login-form";
        }

        return "error-page";
    }
}
