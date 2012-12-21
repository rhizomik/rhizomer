package net.rhizomik.rhizomer.authentication;
/*[<<IFilter>>]^-.-[FiltreRizhomik|LoginFormulari;FOAFSSL|init();doFilter();destroy()], [FiltreRizhomik]-->[FOAFSSL], [FiltreRizhomik]-->[LoginFormulari|nuse|mostrar_formulari();validar_dades()]
[Iterator]-->[LoginFormulari]
[Iterator]^-.-[IteratorFitxer]*/
import java.io.IOException;
import java.security.cert.X509Certificate;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import net.java.dev.sommer.foafssl.j2ee.filter.FoafSSLFilter;
import net.rhizomik.rhizomer.authentication.Constants.Constants;

/**
 * Classe que valida que estiguis identificat mitjançant un certificat o a través d'un login
 * @author <mtp1268@gmail.com>
 */
public class FiltreRhizomik implements Filter {
    private LoginFormulari login = null;
    private final FoafSSLFilter foaf_ssl = new FoafSSLFilter();
    private X509Certificate x509Certificate;

    public void init(FilterConfig filterConfig) throws ServletException {
        this.foaf_ssl.init(filterConfig);
        this.login = new LoginFormulari(filterConfig.getServletContext().getRealPath("WEB-INF"));
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpSession session = ((HttpServletRequest)request).getSession(true);
        if (request.getAttribute("javax.servlet.request.X509Certificate") == null) {
            request.setAttribute("javax.servlet.request.X509Certificate", new X509Certificate[]{this.x509Certificate});
            if (this.login.sha_de_autentificar(request)) {
                this.login.mostrar_formulari(response);
            } else {
                this.login.obtenir_dades();
                try{
                    if (this.login.son_valides()) {
                        session.setAttribute(Constants.NOM_SESSIO_USUARI, this.login.getUsuari());
                        chain.doFilter(request, response);
                    } else {
                        this.login.mostrar_formulari(response);
                    }
                }catch(NullPointerException ex){
                    System.out.println(ex.getMessage());
                }
            }
        } else {
            X509Certificate cert[] = (X509Certificate[])request.getAttribute("javax.servlet.request.X509Certificate");
            session.setAttribute(Constants.NOM_SESSIO_USUARI, cert[0].getSubjectDN().toString());
            this.foaf_ssl.doFilter(request, response, chain);
        }
    }
    public void destroy() {
        this.foaf_ssl.destroy();
    }
}