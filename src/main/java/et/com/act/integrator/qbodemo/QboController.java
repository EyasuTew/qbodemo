//package et.com.act.integrator.qbodemo;
//
//import com.intuit.ipp.core.Context;
//import com.intuit.ipp.core.ServiceType;
//import com.intuit.ipp.data.Customer;
//import com.intuit.ipp.exception.FMSException;
//import com.intuit.ipp.exception.InvalidTokenException;
//import com.intuit.ipp.security.OAuth2Authorizer;
//import com.intuit.ipp.services.DataService;
//import com.intuit.ipp.services.QueryResult;
//import com.intuit.oauth2.client.OAuth2PlatformClient;
//import com.intuit.oauth2.config.Environment;
//import com.intuit.oauth2.config.OAuth2Config;
//import com.intuit.oauth2.config.Scope;
//import com.intuit.oauth2.data.BearerTokenResponse;
//import com.intuit.oauth2.exception.InvalidRequestException;
//import com.intuit.oauth2.exception.OAuthException;
//import jakarta.servlet.http.HttpSession;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.UUID;
//
//@Controller
//public class QboController {
//
//    @Value("${qbo.client.id}")
//    private String clientId;
//
//    @Value("${qbo.client.secret}")
//    private String clientSecret;
//
//    @Value("${qbo.redirect.uri}")
//    private String redirectUri;
//
//    // Entry point, shows the connect button
//    @GetMapping("/")
//    public String home() {
//        return "index";
//    }
//
//    // Kicks off the OAuth 2.0 flow
//    @GetMapping("/connect")
//    public String connectToQbo(HttpSession session) {
//        try {
//            OAuth2Config oauth2Config = new OAuth2Config.OAuth2ConfigBuilder(clientId, clientSecret)
////                    .callBackURL(redirectUri)
//                    .callDiscoveryAPI(Environment.SANDBOX) // call discovery API to populate urls
//                    .buildConfig();
//
//            String csrfToken = oauth2Config.generateCSRFToken();
////            session.setAttribute("csrfToken", csrfToken);
//
//            List<Scope> scopes = new ArrayList<>();
////            scopes.add(Scope.OpenIdAll);
////            scopes.add(Scope.Accounting);
////            scopes.add(Scope.Profile);
////            scopes.add(Scope.Email);
////            scopes.add(Scope.Phone);
//            scopes.add(Scope.All);
//
//
//            String authorizationUrl = oauth2Config.prepareUrl(scopes, redirectUri, csrfToken);
//            return "redirect:" + authorizationUrl;
//
//        }
////        catch (InvalidTokenException e) {
////            // This exception is for token validation, not usually thrown here
////            e.printStackTrace();
////            return "error";
////        }
//        catch (InvalidRequestException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    // This is the redirect URI from Intuit
//    @GetMapping("/integrations/qbo/redirect")
//    public String oauth2Callback(@RequestParam("code") String authCode, @RequestParam("state") String state,
//                                 @RequestParam("realmId") String realmId, HttpSession session) {
//
////        String csrfToken = (String) session.getAttribute("csrfToken");
////        if (csrfToken == null || !csrfToken.equals(state)) {
////            return "error"; // CSRF token mismatch
////        }
//
//        try {
//            OAuth2Config oauth2Config = new OAuth2Config.OAuth2ConfigBuilder(clientId, clientSecret)
//                    .callDiscoveryAPI(Environment.SANDBOX)
//                    .buildConfig();
//
//            OAuth2PlatformClient platformClient = new OAuth2PlatformClient(oauth2Config);
//            BearerTokenResponse bearerTokenResponse = platformClient.retrieveBearerTokens(authCode, redirectUri);
//
//            // Store tokens and realmId in the session
//            session.setAttribute("accessToken", bearerTokenResponse.getAccessToken());
//            session.setAttribute("realmId", realmId);
//
//            return "redirect:/dashboard";
//        } catch (OAuthException e) {
//            e.printStackTrace();
//            return "error";
//        }
//    }
//
//    // Display the data after successful connection
//    @GetMapping("/dashboard")
//    public String showDashboard(HttpSession session, Model model) {
//        String accessToken = (String) session.getAttribute("accessToken");
//        String realmId = (String) session.getAttribute("realmId");
//
//        if (accessToken == null || realmId == null) {
//            return "redirect:/"; // Not connected, redirect home
//        }
//
//        try {
//            // Create the DataService
//            OAuth2Config oauth2Config = new OAuth2Config.OAuth2ConfigBuilder(clientId, clientSecret)
//                    .callDiscoveryAPI(Environment.SANDBOX)
//                    .buildConfig();
//
//            OAuth2Authorizer oauth = new OAuth2Authorizer(accessToken);
//            Context context = new Context(oauth, ServiceType.QBO, realmId);
//            DataService service = new DataService(context);
//
//            // Get Customers
//            String customerSql = "SELECT * FROM Customer WHERE Job = false";
//            QueryResult customerResult = service.executeQuery(customerSql);
//            List<Customer> customers = (List<Customer>) customerResult.getEntities();
//            model.addAttribute("customers", customers);
//
//            // Get Projects (Jobs)
//            String projectSql = "SELECT * FROM Customer WHERE Job = true";
//            QueryResult projectResult = service.executeQuery(projectSql);
//            List<Customer> projects = (List<Customer>) projectResult.getEntities();
//            model.addAttribute("projects", projects);
//
//        } catch (FMSException e) {
//            e.printStackTrace();
//            return "error";
//        }
//
//        return "dashboard";
//    }
//}