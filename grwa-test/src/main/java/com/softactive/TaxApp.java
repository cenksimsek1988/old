package com.softactive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.context.request.RequestContextListener;

@SpringBootApplication
//@EnableJpaRepositories(basePackages= "com.softactive")
//@ComponentScan(basePackages= "com.softactive")
//@EntityScan(basePackages= "com.softactive")
public class TaxApp 
//extends SpringBootServletInitializer
implements CommandLineRunner
{	
	@Autowired @Lazy
	Example example;
	public static void main(String[] args) {
		SpringApplication.run(TaxApp.class, args);
	}

//	@Override
//	public void onStartup(final ServletContext sc) throws ServletException {
//		AnnotationConfigWebApplicationContext root = new AnnotationConfigWebApplicationContext();
//		root.scan("com.softactive");
//		sc.addListener(new ContextLoaderListener(root));
//		ServletRegistration.Dynamic appServlet = sc.addServlet("mvc", new DispatcherServlet(new GenericWebApplicationContext()));
//		appServlet.setLoadOnStartup(1);
//	}

	@Override
	public void run(String... args) throws Exception {
		example.update();
	}

	@Bean
	public RequestContextListener requestContextListener() {
		return new RequestContextListener();
	}
}