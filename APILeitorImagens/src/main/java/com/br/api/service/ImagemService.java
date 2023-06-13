package com.br.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@Service
public class ImagemService {

    @Value("${leitor.diretorio.raiz}")
    private String raiz;

    @Value("${leitor.diretorio.imagem}")
    private String diretorioFotos;

    public void salvarImagem(MultipartFile arquivo){
        this.salvar(this.diretorioFotos, arquivo);
    }

     public void salvar(String diretorio, MultipartFile arquivo){
        Path diretorioPath = Paths.get(this.raiz,diretorio);
         Path arquivoPath = null;
        if(Objects.nonNull(arquivo.getOriginalFilename())){
            arquivoPath = diretorioPath.resolve(arquivo.getOriginalFilename());
        }

        try{
            Files.createDirectories(diretorioPath);
            arquivo.transferTo(arquivoPath.toFile());
        } catch (Exception e){
            throw new RuntimeException("Não foi possível fazer o Upload da imagem");
        }
    }
}
