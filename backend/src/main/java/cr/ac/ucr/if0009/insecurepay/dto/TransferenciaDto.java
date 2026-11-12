package cr.ac.ucr.if0009.insecurepay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class TransferenciaDto {

    @NotNull
    private Long cuentaOrigenId;

    @NotBlank
    @Size(max = 30)
    private String numeroCuentaDestino;

    @NotNull
    @DecimalMin(value = "0.01")
    @Digits(integer = 13, fraction = 2)
    private BigDecimal monto;

    @Size(max = 250)
    private String conceptoHtml;

    public TransferenciaDto() {
    }

    public TransferenciaDto(Long cuentaOrigenId, String numeroCuentaDestino, BigDecimal monto, String conceptoHtml) {
        this.cuentaOrigenId = cuentaOrigenId;
        this.numeroCuentaDestino = numeroCuentaDestino;
        this.monto = monto;
        this.conceptoHtml = conceptoHtml;
    }

    public Long getCuentaOrigenId() {
        return cuentaOrigenId;
    }

    public void setCuentaOrigenId(Long cuentaOrigenId) {
        this.cuentaOrigenId = cuentaOrigenId;
    }

    public String getNumeroCuentaDestino() {
        return numeroCuentaDestino;
    }

    public void setNumeroCuentaDestino(String numeroCuentaDestino) {
        this.numeroCuentaDestino = numeroCuentaDestino;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getConceptoHtml() {
        return conceptoHtml;
    }

    public void setConceptoHtml(String conceptoHtml) {
        this.conceptoHtml = conceptoHtml;
    }
}
