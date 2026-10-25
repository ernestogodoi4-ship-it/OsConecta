# OS Conecta para Android

Aplicativo Android que abre o catálogo de modelos de encerramento de OS em uma WebView local. Os modelos e dados são processados no aparelho; o app oferece cópia e compartilhamento do texto montado.

## Gerar APK

O repositório inclui GitHub Actions em `.github/workflows/build.yml`. Depois de enviar o projeto para o GitHub:

1. Abra a aba **Actions**.
2. Execute **Build APK** (ou aguarde a compilação após um push).
3. Baixe o artefato **OSConecta-apk** da execução concluída.
4. O arquivo `app-debug.apk` estará dentro do ZIP do artefato.

O APK debug pode ser instalado para testes no Android. Uma versão release distribuível requer assinatura própria.

## Compilação local

Requer JDK 17, Android SDK (API 34) e Gradle 8.7. Na raiz do projeto, execute:

```sh
gradle assembleDebug
```

Saída: `app/build/outputs/apk/debug/app-debug.apk`.

