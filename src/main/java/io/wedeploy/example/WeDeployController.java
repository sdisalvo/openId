package io.wedeploy.example;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.http.HttpHeaders.USER_AGENT;

@Controller
@EnableAutoConfiguration
public class WeDeployController {

    private static final Logger log = Logger.getLogger( WeDeployController.class.getName() );
    private static final String GOOGLE_OAUTH = "https://www.googleapis.com/oauth2/v4/token";

    public WeDeployController() {

    }

    public static void main(String[] args) {
        SpringApplication.run(WeDeployController.class, args);
    }

    @RequestMapping("/")
    public ModelAndView hello() {
        return new ModelAndView("layout");
    }


    @RequestMapping(value = "/openId", method = RequestMethod.GET)
    public @ResponseBody
    void generateReport(
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            String email = (String) request.getSession().getAttribute("email");
            String code = request.getParameter("code");
            log.info("code: " + code + " email: " + email);
            if (email == null && code != null && code.length() > 0) {
                // Send post request to google oauth
                HttpClient client = HttpClientBuilder.create().build();
                HttpPost post = new HttpPost(GOOGLE_OAUTH);
                post.setHeader("User-Agent", USER_AGENT);
                List<NameValuePair> urlParameters = new ArrayList<>();
                urlParameters.add(new BasicNameValuePair("code", code));
                urlParameters.add(new BasicNameValuePair("client_id", "105248247635-270o2p37be66bbmhd7dt7nhshqu6ug2l.apps.googleusercontent.com"));
                urlParameters.add(new BasicNameValuePair("client_secret", "FmYfHYybJ7YP6fQLNny6XKjp&redirect_uri=https%3A%2F%2Fsaxapi-181710.appspot.com%2F"));
                urlParameters.add(new BasicNameValuePair("grant_type", "authorization_code"));
                post.setEntity(new UrlEncodedFormEntity(urlParameters));
                HttpResponse res = client.execute(post);
                log.info("Response Code from Google oauth: " + res.getStatusLine().getStatusCode());
                BufferedReader rd = new BufferedReader(new InputStreamReader(res.getEntity().getContent()));
                StringBuffer result = new StringBuffer();
                String line = "";
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }

                // read response
                JSONObject json = new JSONObject(line);
                String idToken = json.getString("id_token");
                log.info("id_token: " + idToken);
                DecodedJWT jwt = JWT.decode(idToken);
                email = jwt.getClaim("email").asString();
                response.getWriter().println("Logged!!! your email address is: " + email);

                request.getSession().setAttribute("email", email);

                response.getWriter().println("Your email is: " + email);

            } else {
                if (email != null) {
                    response.getWriter().println("already logged: " + email);
                } else {
                    response.getWriter().println("<html><p><a title=\"login google\" href=\"https://accounts.google.com/o/oauth2/v2/auth?client_id=105248247635-270o2p37be66bbmhd7dt7nhshqu6ug2l.apps.googleusercontent.com&amp;response_type=code&amp;scope=openid%20email&amp;redirect_uri=https://tvk5jwi-testgoogleopenid.wedeploy.io/openId&amp;state=ciaociao&amp;login_hint=sdisalvo@gmail.com&amp;nonce=0394852-3190485-2490001\">login with google</a></p></html>");
                }
            }

        } catch ( Exception x ) {
            log.log( Level.SEVERE, "Error: ", x);
        }

    }


}