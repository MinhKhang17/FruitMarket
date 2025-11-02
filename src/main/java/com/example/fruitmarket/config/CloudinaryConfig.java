package com.example.fruitmarket.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dt1vxcxdw",
                "api_key", "687268262255632",
                "api_secret", "q4hktNoHjL8GOAkHO-MKJS8GeIg",
                "secure", true
        ));
    }
}
