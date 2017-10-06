package io.wedeploy.example;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Controller
@EnableAutoConfiguration
public class WeDeployController {

    private static final Logger log = Logger.getLogger( WeDeployController.class.getName() );
    private static final String GOOGLE_OAUTH = "https://www.googleapis.com/oauth2/v4/token";

    public WeDeployController() { }

    public static void main(String[] args) {
        SpringApplication.run(WeDeployController.class, args);
    }

    @RequestMapping("/")
    public ModelAndView hello() {
        return new ModelAndView("layout");
    }

    // First call. Redirect user to google
    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public @ResponseBody void login( HttpServletRequest request, HttpServletResponse response) {

        // Generate a random state to veryfy source in incoming google call
        String state = new BigInteger(130, new SecureRandom()).toString(32);
        request.getSession().setAttribute("state", state);
        try {
            response.getWriter().println(
                    "<html><p><a title=\"login google\" href=\"https://accounts.google.com/o/oauth2/v2/auth?client_id=105248247635-270o2p37be66bbmhd7dt7nhshqu6ug2l.apps.googleusercontent.com&amp;response_type=code&amp;scope=openid%20email&amp;redirect_uri=https://openid-testgoogleopenid.wedeploy.io/openId&amp;" +
                            "state=" + state + "&amp;login_hint=sdisalvo@gmail.com&amp;nonce=0394852-3190485-2490001\">login with google</a></p></html>");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @RequestMapping(value = "/openId", method = RequestMethod.GET)
    public @ResponseBody void generateReport( HttpServletRequest request, HttpServletResponse response) {
        try {
            String code = request.getParameter("code");
            String state = request.getParameter("state");
            String email = (String) request.getSession().getAttribute("email");

            log.info("code: " + code + " email: " + email + "state = " + state);

            // call from google
            if (state != null && code != null) {
                // verify incoming call
                String stateSession = request.getSession().getAttribute("state").toString();
                if (stateSession.equals(state) == false) {
                    throw new Exception("Invalid state found.");
                }

                // Send request to Google oauth to verify login
                CloseableHttpClient client = HttpClientBuilder.create().build();
                HttpPost post = new HttpPost(GOOGLE_OAUTH);
                post.setHeader("Content-Type", "application/x-www-form-urlencoded");
                List<NameValuePair> urlParameters = new ArrayList<>();
                urlParameters.add(new BasicNameValuePair("code", code));
                urlParameters.add(new BasicNameValuePair("client_id", "105248247635-270o2p37be66bbmhd7dt7nhshqu6ug2l.apps.googleusercontent.com"));
                urlParameters.add(new BasicNameValuePair("client_secret", "FmYfHYybJ7YP6fQLNny6XKjp"));
                urlParameters.add(new BasicNameValuePair("grant_type", "authorization_code"));
                urlParameters.add(new BasicNameValuePair("redirect_uri", "https://openid-testgoogleopenid.wedeploy.io/openId"));
                post.setEntity(new UrlEncodedFormEntity(urlParameters));
                CloseableHttpResponse res = client.execute(post);
                log.info("Response Code from Google oauth: " + res.getStatusLine());
                HttpEntity entity = res.getEntity();
                String responseString = EntityUtils.toString(entity);
                log.info("Response body from Google oauth: " + responseString);

                // read response
                JSONObject json = new JSONObject(responseString);
                String idToken = json.getString("id_token");
                log.info("id_token: " + idToken);
                DecodedJWT jwt = JWT.decode(idToken);
                email = jwt.getClaim("email").asString();
                response.getWriter().println("Logged!!! your email address is: " + email);

                request.getSession().setAttribute("email", email);

                response.getWriter().println("Your email is: " + email);

            } else if (email != null) {
                // second call from autenticated user
                response.getWriter().println("already logged: " + email);
            }
        } catch (Exception x) {
            log.log(Level.SEVERE, "Error: ", x);
        }
    }
}