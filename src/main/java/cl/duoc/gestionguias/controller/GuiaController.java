package cl.duoc.gestionguias.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import cl.duoc.gestionguias.consumer.GuiaConsumerService;
import cl.duoc.gestionguias.entity.GuiaDespacho;
import cl.duoc.gestionguias.entity.GuiaProcesada;
import cl.duoc.gestionguias.service.GuiaService;

@RestController
@RequestMapping("/guias")
public class GuiaController {

    @Autowired
    private GuiaService guiaService;

    @Autowired
    private GuiaConsumerService guiaConsumerService;

    // Endpoint adicional: consume los mensajes pendientes de la Cola 1
    // de RabbitMQ y los guarda en la tabla "guias_procesadas" (rol GESTION)
    @PostMapping("/cola1/procesar")
    public List<GuiaProcesada> procesarColaUno() {
        return guiaConsumerService.procesarMensajesColaUno();
    }

    // Listar todas las guias (rol GESTION)
    @GetMapping
    public List<GuiaDespacho> obtenerGuias() {
        return guiaService.obtenerGuias();
    }

    // Crear guia (rol GESTION)
    @PostMapping
    public GuiaDespacho crearGuia(@RequestBody GuiaDespacho guia) {
        return guiaService.crearGuia(guia);
    }

    // Buscar por ID (rol GESTION)
    @GetMapping("/{id}")
    public GuiaDespacho buscarPorId(@PathVariable Long id) {
        return guiaService.buscarPorId(id);
    }

    // Actualizar guia (rol GESTION)
    @PutMapping("/{id}")
    public GuiaDespacho actualizarGuia(
            @PathVariable Long id,
            @RequestBody GuiaDespacho guia) {
        return guiaService.actualizarGuia(id, guia);
    }

    // Eliminar guia (rol GESTION)
    @DeleteMapping("/{id}")
    public String eliminarGuia(@PathVariable Long id) {
        boolean eliminada = guiaService.eliminarGuia(id);
        return eliminada ? "Guía eliminada correctamente" : "No se encontró la guía";
    }

    // Descargar archivo desde S3 (rol DESCARGA)
    @GetMapping("/{id}/descargar")
    public ResponseEntity<byte[]> descargarGuia(@PathVariable Long id) {
        byte[] archivo = guiaService.descargarGuia(id);
        if (archivo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=guia-" + id + ".txt")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(archivo);
    }

    // Subir archivo externo a S3 (rol GESTION)
    @PostMapping("/{id}/subir")
    public ResponseEntity<GuiaDespacho> subirArchivo(
            @PathVariable Long id,
            @RequestParam MultipartFile file) {
        GuiaDespacho guia = guiaService.subirArchivo(id, file);
        if (guia == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(guia);
    }

    // Consultar por transportista (rol GESTION)
    @GetMapping("/transportista/{transportista}")
    public List<GuiaDespacho> buscarPorTransportista(@PathVariable String transportista) {
        return guiaService.buscarPorTransportista(transportista);
    }

    // Consultar por fecha (rol GESTION)
    @GetMapping("/fecha/{fecha}")
    publ