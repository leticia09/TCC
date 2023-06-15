package com.br.api.controller;

import com.br.api.service.ImagemService;
import com.br.api.service.LeitorService;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/upload")
public class LeitorController {

    @Autowired
    LeitorService leitorService;

    @Autowired
    ImagemService imagemService;

    @CrossOrigin(origins = "http://localhost:4200")
    @PostMapping()
    public List<String> leitor(@RequestParam MultipartFile imagem) throws TesseractException, IOException {
        return leitorService.read(imagem.getOriginalFilename());
    }

    @GetMapping(value = "/verify")
    public String verify() {
        return leitorService.verify();
    }

}
