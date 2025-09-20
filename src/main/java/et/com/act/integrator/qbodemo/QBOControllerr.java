package et.com.act.integrator.qbodemo;

import com.intuit.ipp.core.Context;
import com.intuit.ipp.core.ServiceType;
import com.intuit.ipp.data.Customer;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.security.OAuth2Authorizer;
import com.intuit.ipp.services.DataService;
import com.intuit.ipp.services.QueryResult;
import com.intuit.oauth2.client.OAuth2PlatformClient;
import com.intuit.oauth2.config.Environment;
import com.intuit.oauth2.config.OAuth2Config;
import com.intuit.oauth2.config.Scope;
import com.intuit.oauth2.data.BearerTokenResponse;
import com.intuit.oauth2.exception.InvalidRequestException;
import com.intuit.oauth2.exception.OAuthException;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.annotation.PostConstruct;
import com.intuit.ipp.util.Config;

import java.util.ArrayList;
import java.util.List;

@Controller
public class QBOControllerr {

    @Value("${qbo.client.id}")
    private String clientId;

    @Value("${qbo.client.secret}")
    private String clientSecret;

    @Value("${qbo.redirect.uri}")
    private String redirectUri;

    private static final Environment ENVIRONMENT = Environment.SANDBOX;
    private static final String SANDBOX_BASE_URL = "https://sandbox-quickbooks.api.intuit.com/v3/company";
    private static final String PRODUCTION_BASE_URL = "https://quickbooks.api.intuit.com/v3/company";

    @PostConstruct
    public void init() {
        // Explicitly set the base URL for Sandbox environment
        System.out.println("=== CONFIGURING SANDBOX ENVIRONMENT ===");
        System.out.println("Setting base URL to: " + SANDBOX_BASE_URL);
        try {
            Config.setProperty(Config.BASE_URL_QBO, SANDBOX_BASE_URL);
            System.out.println("Successfully configured Sandbox base URL");
            // Also set other optional configuration properties
//            Config.setProperty(Config.SERIALIZATION_REQUEST_FORMAT, Config.);
//            Config.setProperty(Config.BASE_URL_QBO, SANDBOX_BASE_URL);
        } catch (Exception e) {
            System.err.println("Error configuring base URL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Entry point, shows the connect button
    @GetMapping("/")
    public String home() {
        return "index";
    }

    // Kicks off the OAuth 2.0 flow
    @GetMapping("/connect")
    public String connectToQbo(HttpSession session) {
        try {
            OAuth2Config oauth2Config = new OAuth2Config.OAuth2ConfigBuilder(clientId, clientSecret)
                    .callDiscoveryAPI(Environment.SANDBOX)
                    .buildConfig();
            String csrfToken = oauth2Config.generateCSRFToken();
            session.setAttribute("csrfToken", csrfToken);
            List<Scope> scopes = new ArrayList<>();
//            scopes.add(Scope.fromValue("com.intuit.quickbooks.accounting"));
            scopes.add(Scope.Accounting);
            scopes.add(Scope.OpenIdAll);
            // Remove Scope.All as it might cause authorization issues
            String authorizationUrl = oauth2Config.prepareUrl(scopes, redirectUri, csrfToken);
            return "redirect:" + authorizationUrl;

        } catch (InvalidRequestException e) {
            e.printStackTrace();
            return "error";
        }
    }

    // This is the redirect URI from Intuit
    @GetMapping("/integrations/qbo/redirect")
    public String oauth2Callback(@RequestParam("code") String authCode,
                                 @RequestParam("state") String state,
                                 @RequestParam("realmId") String realmId,
                                 HttpSession session) {

        String csrfToken = (String) session.getAttribute("csrfToken");
        if (csrfToken == null || !csrfToken.equals(state)) {
            return "error"; // CSRF token mismatch
        }
        try {
            OAuth2Config oauth2Config = new OAuth2Config.OAuth2ConfigBuilder(clientId, clientSecret)
                    .callDiscoveryAPI(Environment.SANDBOX)
                    .buildConfig();
            OAuth2PlatformClient platformClient = new OAuth2PlatformClient(oauth2Config);
            BearerTokenResponse bearerTokenResponse = platformClient.retrieveBearerTokens(authCode, redirectUri);
            // Store all tokens and realmId in the session
            session.setAttribute("accessToken", bearerTokenResponse.getAccessToken());
            session.setAttribute("refreshToken", bearerTokenResponse.getRefreshToken());
            session.setAttribute("realmId", realmId);
            session.setAttribute("tokenExpiry", System.currentTimeMillis() +
                    (bearerTokenResponse.getExpiresIn() * 1000));

            return "redirect:/dashboard";
        } catch (OAuthException e) {
            e.printStackTrace();
            return "error";
        }
    }

    // Display the data after successful connection
    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        String accessToken = getValidAccessToken(session);
        String realmId = (String) session.getAttribute("realmId");
        if (accessToken == null || realmId == null) {
            return "redirect:/connect"; // Not connected or token expired, redirect to connect
        }
        try {
            Config.setProperty(Config.BASE_URL_QBO, SANDBOX_BASE_URL);
            // Create the DataService
            DataService service = getDataService(accessToken, realmId);
            // Get Customers
            String customerSql = "SELECT * FROM Customer WHERE Job = false and IsProject = false";
            QueryResult customerResult = service.executeQuery(customerSql);
            List<Customer> customers = (List<Customer>) customerResult.getEntities();
            model.addAttribute("customers", customers);
            // Get Projects (Jobs)
            String projectSql = "SELECT * FROM Customer WHERE IsProject = true";
            QueryResult projectResult = service.executeQuery(projectSql);
            List<Customer> projects = (List<Customer>) projectResult.getEntities();
            model.addAttribute("projects", projects);
        } catch (FMSException e) {
            e.printStackTrace();
            // If there's an FMS exception, it might be due to token issues
            session.invalidate();
            return "redirect:/connect";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }

        return "dashboard";
    }

    @PostMapping("/addCustomer")
    public String addCustomer(@RequestParam("displayName") String displayName,
                              @RequestParam("email") String email,
                              @RequestParam("phone") String phone,
                              HttpSession session) {

        Config.setProperty(Config.BASE_URL_QBO, SANDBOX_BASE_URL);

        String accessToken = getValidAccessToken(session);
        String realmId = (String) session.getAttribute("realmId");
        if (accessToken == null || realmId == null) {
            return "redirect:/connect";
        }
        try {
            DataService service = getDataService(accessToken, realmId);
            Customer customer = new Customer();
            customer.setDisplayName(displayName);
            customer.setPrimaryEmailAddr(new com.intuit.ipp.data.EmailAddress());
            customer.getPrimaryEmailAddr().setAddress(email);
            customer.setPrimaryPhone(new com.intuit.ipp.data.TelephoneNumber());
            customer.getPrimaryPhone().setFreeFormNumber(phone);
            service.add(customer);
        } catch (FMSException e) {
            e.printStackTrace();
            return "error";
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/addProject")
    public String addProject(@RequestParam("displayName") String displayName,
                             @RequestParam("parentRefId") String parentRefId,
                             HttpSession session) {

        Config.setProperty(Config.BASE_URL_QBO, SANDBOX_BASE_URL);

        String accessToken = getValidAccessToken(session);
        String realmId = (String) session.getAttribute("realmId");
        if (accessToken == null || realmId == null) {
            return "redirect:/connect";
        }
        try {
            DataService service = getDataService(accessToken, realmId);
            Customer project = new Customer();
            project.setDisplayName(displayName);
            project.setIsProject(true);
            project.setJob(true); // Mark as a job
//            project.set

            com.intuit.ipp.data.ReferenceType parentRef = new com.intuit.ipp.data.ReferenceType();
            parentRef.setValue(parentRefId);
            project.setParentRef(parentRef);
            service.add(project);
        } catch (FMSException e) {
            e.printStackTrace();
            return "error";
        }
        return "redirect:/dashboard";
    }

    private DataService getDataService(String accessToken, String realmId) throws FMSException {
        OAuth2Authorizer oauth = new OAuth2Authorizer(accessToken);
        Context context = new Context(oauth, ServiceType.QBO, realmId);
        //context.setauth(oauth).realmId(realmId).serviceType(ServiceType.QBO).build();
        return new DataService(context);
    }

    private String getValidAccessToken(HttpSession session) {
        String accessToken = (String) session.getAttribute("accessToken");
        Long tokenExpiry = (Long) session.getAttribute("tokenExpiry");
        if (accessToken == null || tokenExpiry == null) {
            return null;
        }
        // Refresh token if it's about to expire (within 5 minutes)
        if (System.currentTimeMillis() > (tokenExpiry - 300000)) {
            try {
                String refreshToken = (String) session.getAttribute("refreshToken");
                if (refreshToken == null) {
                    return null;
                }
                OAuth2Config oauth2Config = new OAuth2Config.OAuth2ConfigBuilder(clientId, clientSecret)
                        .callDiscoveryAPI(Environment.SANDBOX)
                        .buildConfig();
                OAuth2PlatformClient platformClient = new OAuth2PlatformClient(oauth2Config);
                BearerTokenResponse newTokens = platformClient.refreshToken(refreshToken);
                // Update session with new tokens
                session.setAttribute("accessToken", newTokens.getAccessToken());
                session.setAttribute("refreshToken", newTokens.getRefreshToken());
                session.setAttribute("tokenExpiry", System.currentTimeMillis() +
                        (newTokens.getExpiresIn() * 1000));
                return newTokens.getAccessToken();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return accessToken;
    }

    // Additional endpoint to handle token refresh manually if needed
    @GetMapping("/refresh")
    public String refreshToken(HttpSession session) {
        String refreshToken = (String) session.getAttribute("refreshToken");
        if (refreshToken == null) {
            return "redirect:/connect";
        }
        try {
            OAuth2Config oauth2Config = new OAuth2Config.OAuth2ConfigBuilder(clientId, clientSecret)
                    .callDiscoveryAPI(Environment.SANDBOX)
                    .buildConfig();
            OAuth2PlatformClient platformClient = new OAuth2PlatformClient(oauth2Config);
            BearerTokenResponse newTokens = platformClient.refreshToken(refreshToken);
            // Update session with new tokens
            session.setAttribute("accessToken", newTokens.getAccessToken());
            session.setAttribute("refreshToken", newTokens.getRefreshToken());
            session.setAttribute("tokenExpiry", System.currentTimeMillis() +
                    (newTokens.getExpiresIn() * 1000));
            return "redirect:/dashboard";
        } catch (Exception e) {
            e.printStackTrace();
            session.invalidate();
            return "redirect:/connect";
        }
    }
}