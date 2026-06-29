package cl.duoc.gestionguias.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import cl.duoc.gestionguias.entity.GuiaDespacho;
import cl.duoc.gestionguias.repository.GuiaRepository;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Service
public class GuiaService {

    @Autowired
    private GuiaRepository guiaRepository;

    @Autowired
    private S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${efs.path}")
    private String efsPath;

    // Crear guia: guarda en EFS y sube a S3
    public GuiaDespacho crearGuia(GuiaDespacho guia) {
        guia.setEstado("GENERADA");
        GuiaDespacho guiaGuardada = guiaRepository.save(guia);

        // 1. Guardar temporalmente en EFS
        String rutaEfs = guardarEnEfs(guiaGuardada);
        guiaGuardada.setRutaEfs(rutaEfs);

        // 2. Subir a S3 organizado por fecha/transportista
        String keyS3 = subirAmazonS3(guiaGuardada, rutaEfs);
        guiaGuardada.setUrlS3(keyS3);

        return guiaRepository.save(guiaGuardada);
    }

    // Guardar archivo en EFS temporalmente
    private String guardarEnEfs(GuiaDespacho guia) {
        String carpeta = efsPath + "/" + guia.getFecha() + "/" + guia.getTransportista();
        File dir = new File(carpeta);
        if (!dir.exists()) dir.mkdirs();

        String rutaArchivo = carpeta + "/" + guia.getNumeroGuia() + ".txt";

        try (FileWriter writer = new FileWriter(rutaArchivo)) {
            writer.write("Número Guía: " + guia.getNumeroGuia() + "\n");
            writer.write("Transportista: " + guia.getTransportista() + "\n");
            writer.write("Fecha: " + guia.getFecha() + "\n");
            writer.write("Estado: " + guia.getEstado() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rutaArchivo;
    }

    // Subir archivo desde EFS a S3
    private String subirAmazonS3(GuiaDespacho guia, String rutaEfs) {
        String keyS3 = guia.getFecha() + "/" + guia.getTransportista() + "/" + guia.getNumeroGuia() + ".txt";

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(keyS3)
                .build();

        s3Client.putObject(request, Paths.get(rutaEfs));

        return keyS3;
    }

    // Descargar archivo desde S3
    public byte[] descargarGuia(Long id) {
        GuiaDespacho guia = buscarPorId(id);
        if (guia == null || guia.getUrlS3() == null) return null;

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(guia.getUrlS3())
                .build();

        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
        return response.asByteArray();
    }

    // Subir archivo externo a S3
    public GuiaDespacho subirArchivo(Long id, MultipartFile file) {
        GuiaDespacho guia = buscarPorId(id);
        if (guia == null) return null;

        String keyS3 = guia.getFecha() + "/" + guia.getTransportista() + "/" + guia.getNumeroGuia() + ".txt";

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(keyS3)
                    .build();

            s3Client.putObject(request,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            guia.setUrlS3(keyS3);
            guia.setEstado("SUBIDA");
            return guiaRepository.save(guia);

        } catch (IOException e) {
            throw new RuntimeException("Error al subir archivo", e);
        }
    }

    public List<GuiaDespacho> obtenerGuias() {
        return guiaRepository.findAll();
    }

    public GuiaDespacho buscarPorId(Long id) {
        Optional<GuiaDespacho> guia = guiaRepository.findById(id);
        return guia.orElse(null);
    }

    public GuiaDespacho actualizarGuia(Long id, GuiaDespacho guiaActualizada) {
        Optional<GuiaDespacho> guiaExistente = guiaRepository.findById(id);

        if (guiaExistente.isPresent()) {
            GuiaDespacho guia = guiaExistente.get();
            guia.setNumeroGuia(guiaActualizada.getNumeroGuia());
            guia.setTransportista(guiaActualizada.getTransportista());
            guia.setFecha(guiaActualizada.getFecha());
            guia.setEstado("ACTUALIZADA");

            String rutaEfs = guardarEnEfs(guia);
            String keyS3 = subirAmazonS3(guia, rutaEfs);
            guia.setRutaEfs(rutaEfs);
            guia.setUrlS3(keyS3);

            return guiaRepository.save(guia);
        }
        return null;
    }

    public boolean eliminarGuia(Long id) {
        GuiaDespacho guia = buscarPorId(id);
        if (guia != null) {
            if (guia.getUrlS3() != null) {
                DeleteObjectRequest request = DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(guia.getUrlS3())
                        .build();
                s3Client.deleteObject(request);
            }
            guiaRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<GuiaDespacho> buscarPorTransportista(String transportista) {
        return guiaRepository.findByTransportista(transportista);
    }

    public List<GuiaDespacho> buscarPorFecha(String fecha) {
        return guiaRepository.findByFecha(fecha);
    }

    public List<GuiaDespacho> buscarPorTransportistaYFecha(String transportista, String fecha) {
        return guiaRepository.findByTransportistaAndFecha(transportista, fecha);
    }
}
