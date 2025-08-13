package org.mobitti.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mobitti.dtos.AppUserDto;
import org.mobitti.dtos.ClientChatMessage;
import org.mobitti.dtos.MobittiChatResponse;
import org.mobitti.helpers.JWTUtils;
import org.mobitti.services.MobittiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AiController extends BaseController{

    @Autowired
    private MobittiChatService mobittiChatService;
    private static final Logger logger = LogManager.getLogger(AiController.class);

    @PostMapping("/chat")
    public MobittiChatResponse chat(@RequestBody ClientChatMessage mobittiChatMessage, @RequestHeader String token) throws IOException, InterruptedException {
        MobittiChatResponse res = new MobittiChatResponse();
        try {
            AppUserDto appUserDto = JWTUtils.validateTokenAndGetUser(token);
            mobittiChatService.chat(appUserDto, mobittiChatMessage, res);
        } catch (Exception e) {
            res.setStatus(handleException(e));
            logger.error(e, e);
        }

        return  res;
    }



}
