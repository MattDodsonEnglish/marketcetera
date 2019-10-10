package com.marketcetera.colin;

import org.marketcetera.admin.AdminClientFactory;
import org.marketcetera.admin.AdminRpcClientFactory;
import org.marketcetera.admin.AdminRpcClientParameters;
import org.marketcetera.admin.PermissionFactory;
import org.marketcetera.admin.RoleFactory;
import org.marketcetera.admin.UserAttributeFactory;
import org.marketcetera.admin.UserFactory;
import org.marketcetera.admin.impl.SimplePermissionFactory;
import org.marketcetera.admin.impl.SimpleRoleFactory;
import org.marketcetera.admin.impl.SimpleUserAttributeFactory;
import org.marketcetera.admin.impl.SimpleUserFactory;
import org.marketcetera.cluster.ClusterDataFactory;
import org.marketcetera.cluster.SimpleClusterDataFactory;
import org.marketcetera.fix.FixAdminClientFactory;
import org.marketcetera.fix.FixAdminRpcClientFactory;
import org.marketcetera.fix.FixAdminRpcClientParameters;
import org.marketcetera.fix.FixSessionAttributeDescriptorFactory;
import org.marketcetera.fix.MutableActiveFixSessionFactory;
import org.marketcetera.fix.MutableFixSessionFactory;
import org.marketcetera.fix.impl.SimpleActiveFixSessionFactory;
import org.marketcetera.fix.impl.SimpleFixSessionAttributeDescriptorFactory;
import org.marketcetera.fix.impl.SimpleFixSessionFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.marketcetera.colin.app.security.SecurityConfiguration;
import com.marketcetera.colin.backend.data.entity.User;
import com.marketcetera.colin.backend.repositories.UserRepository;
import com.marketcetera.colin.backend.service.UserService;
import com.marketcetera.colin.ui.MainView;

/**
 * Spring boot web application initializer.
 */
@SpringBootApplication(scanBasePackageClasses = { SecurityConfiguration.class, MainView.class, Application.class,UserService.class }, exclude = ErrorMvcAutoConfiguration.class)
@EnableJpaRepositories(basePackageClasses = { UserRepository.class })
@EntityScan(basePackageClasses = { User.class })
public class Application
        extends SpringBootServletInitializer
{
    public static void main(String[] args)
    {
        SpringApplication.run(Application.class,
                              args);
    }
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }
    /**
     * Get the admin client factory value.
     *
     * @return an <code>AdminClientFactory&lt;AdminRpcClientParameters&gt;</code> value
     */
    @Bean
    public AdminClientFactory<AdminRpcClientParameters> getAdminClientFactory()
    {
        return new AdminRpcClientFactory();
    }
    /**
     * Get the fix admin client factory value.
     *
     * @return a <code>FixAdminClientFactory&lt;FixAdminRpcClientParameters&gt;</code> value
     */
    @Bean
    public FixAdminClientFactory<FixAdminRpcClientParameters> getFixAdminClientFactory()
    {
        return new FixAdminRpcClientFactory();
    }
    /**
     * Get the fix session attribute descriptor factory value.
     *
     * @return a <code>FixSessionAttributeDescriptorFactory</code> value
     */
    @Bean
    public FixSessionAttributeDescriptorFactory getFixSessionAttributeDescriptorFactory()
    {
        return new SimpleFixSessionAttributeDescriptorFactory();
    }
    /**
     * Get the active fix session factory value.
     *
     * @return a <code>MutableActiveFixSessionFactory</code> value
     */
    @Bean
    public MutableActiveFixSessionFactory getMutableActiveFixSessionFactory()
    {
        return new SimpleActiveFixSessionFactory();
    }
    /**
     * Get the fix session factory value.
     *
     * @return a <code>MutableFixSessionFactory</code> value
     */
    @Bean
    public MutableFixSessionFactory getMutableFixSessionFactory()
    {
        return new SimpleFixSessionFactory();
    }
    /**
     * Get the cluster data factory value.
     *
     * @return a <code>ClusterDataFactory</code> value
     */
    @Bean
    public ClusterDataFactory getClusterDataFactory()
    {
        return new SimpleClusterDataFactory();
    }
    /**
     * Get the user attribute factory value.
     *
     * @return a <code>UserAttribute</code> value
     */
    @Bean
    public UserAttributeFactory getUserAttributeFactory()
    {
        return new SimpleUserAttributeFactory();
    }
    /**
     * Get the permission factory value.
     *
     * @return a <code>PermissionFactory</code> value
     */
    @Bean
    public PermissionFactory getPermissionFactory()
    {
        return new SimplePermissionFactory();
    }
    /**
     * Get the user factory value.
     *
     * @return a <code>UserFactory</code> value
     */
    @Bean
    public UserFactory getUserFactory()
    {
        return new SimpleUserFactory();
    }
    /**
     * Get the role factory value.
     *
     * @return a <code>RoleFactory</code> value
     */
    @Bean
    public RoleFactory getRoleFactory()
    {
        return new SimpleRoleFactory();
    }
}