package org.example.face;


import org.opencv.core.Core;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;




@SpringBootApplication
public class FaceApplication {

    public static void main(String[] args)  {
        SpringApplication.run(FaceApplication.class, args);
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
}
