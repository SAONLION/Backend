package mcm.mcmAI.global.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mcm.mcmAI.global.exception.BusinessException;
import mcm.mcmAI.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// 직원용 태블릿은 IP가 고정되지 않아 IP 화이트리스트 대신 고정 토큰으로 인증한다.
// 토큰은 app.staff-board.token(STAFF_BOARD_TOKEN 환경변수)으로 주입되며, 값이 비어 있으면 모든 요청을 차단한다.
@Component
public class StaffBoardTokenInterceptor implements HandlerInterceptor {

    public static final String TOKEN_HEADER = "X-Staff-Token";

    @Value("${app.staff-board.token:}")
    private String staffBoardToken;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String providedToken = request.getHeader(TOKEN_HEADER);

        if (staffBoardToken == null || staffBoardToken.isBlank() || !staffBoardToken.equals(providedToken)) {
            throw new BusinessException(ErrorCode.STAFF_TOKEN_INVALID);
        }

        return true;
    }
}
