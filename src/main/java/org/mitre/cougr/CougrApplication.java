/*******************************************************************************
 * Copyright 2016 The MITRE Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.mitre.cougr;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class CougrApplication {

    public static void main(String[] args) {
        SpringApplication.run(CougrApplication.class, args);
    }
    
    @Bean
    public static PropertyPlaceholderConfigurer properties() {
    	PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
    	ClassPathResource[] resources = new ClassPathResource[] { new ClassPathResource("websecurity.properties"), new ClassPathResource("db.properties") };
    	ppc.setLocations(resources);
    	ppc.setIgnoreUnresolvablePlaceholders(true);
    	return ppc;
    }
   
}
