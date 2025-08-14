package org.mobitti.helpers;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.apache.commons.codec.binary.StringUtils;
import org.mobitti.dtos.AppUserDto;

import java.nio.charset.StandardCharsets;

public class JWTUtils {


    public static final String MOBITTI_AI_SECRET_KEY = "m/`EIRBsa|T2>'hV=B#k;5onmI<qnw/i=87UsbSt7~_|B8JRT[Z9oxTLj9XC9Gw+am/`EIRBsa|T2>'hV=B#k;5onmI<qnw/i=87UsbSt7~_|B8JRT[Z9oxTLj9XC9Gw+a";
    public static final String MOBITTI_AI_SUBJECT="mobittiAi";

    public static void main(String[] args) {
        System.out.println(validateTokenAndGetUser(""));
    }

    public static AppUserDto validateTokenAndGetUser(String token) {

        try {

            Jws<Claims> claims = Jwts.parser().setSigningKey( MOBITTI_AI_SECRET_KEY.getBytes(StandardCharsets.UTF_8)).parseClaimsJws(token);
            validateClaims(claims);
            return buildUser(claims);
        } catch (Exception e) {
            throw new RuntimeException("Invalid token");
        }
    }

    private static AppUserDto buildUser(Jws<Claims> claims) {
        AppUserDto appUserDto = new AppUserDto();
        appUserDto.setClubId(claims.getBody().get("clubId",Integer.class));
        appUserDto.setEmail(claims.getBody().get("email", String.class));
        appUserDto.setFirstName(claims.getBody().get("firstName", String.class));
        appUserDto.setLastName(claims.getBody().get("lastName", String.class));
        appUserDto.setDepartment(claims.getBody().get("department", String.class));
        appUserDto.setRoleName(claims.getBody().get("roleName", String.class));
        appUserDto.setSuperClubId(claims.getBody().get("superClubId", Integer.class));
        appUserDto.setUserId(claims.getBody().get("userId", String.class));
        appUserDto.setStorageId(claims.getBody().get("storageId", String.class));
        appUserDto.setBirthday(claims.getBody().get("birthday", String.class));
        return appUserDto;
    }

    private static void validateClaims(Jws<Claims> claims) {
        if(claims.getBody().getExpiration().getTime() < System.currentTimeMillis()){
            throw new RuntimeException("Token expired");
        }
        if(!StringUtils.equals(claims.getBody().getSubject(),MOBITTI_AI_SUBJECT)){
            throw new RuntimeException("Invalid token");
        }

    }
}
