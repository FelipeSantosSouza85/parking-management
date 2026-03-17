package com.estapar.parking_management.parking.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PricingAdjustmentPolicy")
class PricingAdjustmentPolicyTest {

    private PricingAdjustmentPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new PricingAdjustmentPolicy();
    }

    @Nested
    @DisplayName("getAdjustmentRate")
    class GetAdjustmentRate {

        @Nested
        @DisplayName("ocupação < 25%")
        class OcupacaoBaixa {

            @Test
            @DisplayName("retorna -0.10 para 0%")
            void retornaMenos10ParaZero() {
                assertThat(policy.getAdjustmentRate(BigDecimal.ZERO)).isEqualByComparingTo("-0.10");
            }

            @Test
            @DisplayName("retorna -0.10 para 24.99%")
            void retornaMenos10Para24_99() {
                assertThat(policy.getAdjustmentRate(new BigDecimal("0.2499"))).isEqualByComparingTo("-0.10");
            }

            @Test
            @DisplayName("retorna -0.10 para 10%")
            void retornaMenos10Para10() {
                assertThat(policy.getAdjustmentRate(new BigDecimal("0.10"))).isEqualByComparingTo("-0.10");
            }
        }

        @Nested
        @DisplayName("ocupação 25% - 50% (até 50% inclusive)")
        class OcupacaoNormal {

            @Test
            @DisplayName("retorna 0.00 para 25%")
            void retornaZeroPara25() {
                assertThat(policy.getAdjustmentRate(new BigDecimal("0.25"))).isEqualByComparingTo("0.00");
            }

            @Test
            @DisplayName("retorna 0.00 entre 25% e 50%")
            void retornaZeroEntre25e50() {
                assertThat(policy.getAdjustmentRate(new BigDecimal("0.35"))).isEqualByComparingTo("0.00");
            }

            @Test
            @DisplayName("retorna 0.00 para 49.99%")
            void retornaZeroPara49_99() {
                assertThat(policy.getAdjustmentRate(new BigDecimal("0.4999"))).isEqualByComparingTo("0.00");
            }

            @Test
            @DisplayName("retorna 0.00 para 50% (até 50% conforme documentação)")
            void retornaZeroPara50() {
                assertThat(policy.getAdjustmentRate(new BigDecimal("0.50"))).isEqualByComparingTo("0.00");
            }
        }

        @Nested
        @DisplayName("ocupação acima de 50% até 75%")
        class OcupacaoAlta {

            @Test
            @DisplayName("retorna +0.10 para 50.01%")
            void retornaMais10Para50_01() {
                assertThat(policy.getAdjustmentRate(new BigDecimal("0.5001"))).isEqualByComparingTo("0.10");
            }

            @Test
            @DisplayName("retorna +0.10 entre 50% e 75%")
            void retornaMais10Entre50e75() {
                assertThat(policy.getAdjustmentRate(new BigDecimal("0.60"))).isEqualByComparingTo("0.10");
            }

            @Test
            @DisplayName("retorna +0.10 para 74.99%")
            void retornaMais10Para74_99() {
                assertThat(policy.getAdjustmentRate(new BigDecimal("0.7499"))).isEqualByComparingTo("0.10");
            }

            @Test
            @DisplayName("retorna +0.10 para 75%")
            void retornaMais10Para75() {
                assertThat(policy.getAdjustmentRate(new BigDecimal("0.75"))).isEqualByComparingTo("0.10");
            }
        }

        @Nested
        @DisplayName("ocupação 75% - 100%")
        class OcupacaoPico {

            @Test
            @DisplayName("retorna +0.25 entre 75.01% e 100%")
            void retornaMais25Entre75e100() {
                assertThat(policy.getAdjustmentRate(new BigDecimal("0.7501"))).isEqualByComparingTo("0.25");
            }

            @Test
            @DisplayName("retorna +0.25 para 90%")
            void retornaMais25Para90() {
                assertThat(policy.getAdjustmentRate(new BigDecimal("0.90"))).isEqualByComparingTo("0.25");
            }

            @Test
            @DisplayName("retorna +0.25 para 100%")
            void retornaMais25Para100() {
                assertThat(policy.getAdjustmentRate(BigDecimal.ONE)).isEqualByComparingTo("0.25");
            }
        }

        @Nested
        @DisplayName("limites de borda")
        class LimitesBorda {

            @Test
            @DisplayName("0% retorna -0.10")
            void zeroRetornaMenos10() {
                assertThat(policy.getAdjustmentRate(BigDecimal.ZERO)).isEqualByComparingTo("-0.10");
            }

            @Test
            @DisplayName("24.99% retorna -0.10")
            void vinteQuatroRetornaMenos10() {
                assertThat(policy.getAdjustmentRate(new BigDecimal("0.2499"))).isEqualByComparingTo("-0.10");
            }

            @Test
            @DisplayName("50.01% retorna +0.10")
            void cinquentaRetornaMais10() {
                assertThat(policy.getAdjustmentRate(new BigDecimal("0.5001"))).isEqualByComparingTo("0.10");
            }

            @Test
            @DisplayName("75.01% retorna +0.25")
            void setentaCincoRetornaMais25() {
                assertThat(policy.getAdjustmentRate(new BigDecimal("0.7501"))).isEqualByComparingTo("0.25");
            }
        }

        @Nested
        @DisplayName("validação de entrada inválida")
        class ValidacaoEntradaInvalida {

            @Test
            @DisplayName("lança exceção para null")
            void lancaExcecaoParaNull() {
                assertThatThrownBy(() -> policy.getAdjustmentRate(null))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("must not be null");
            }

            @Test
            @DisplayName("lança exceção para valor negativo")
            void lancaExcecaoParaNegativo() {
                assertThatThrownBy(() -> policy.getAdjustmentRate(new BigDecimal("-0.01")))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("between 0 and 1");
            }

            @Test
            @DisplayName("lança exceção para valor maior que 1")
            void lancaExcecaoParaMaiorQue1() {
                assertThatThrownBy(() -> policy.getAdjustmentRate(new BigDecimal("1.01")))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("between 0 and 1");
            }
        }
    }
}
