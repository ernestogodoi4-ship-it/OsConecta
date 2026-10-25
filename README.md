# OS Conecta para Android

Aplicativo Android para gerar encerramentos de OS de telecomunicações. Ao iniciar, o app busca a interface HTML mais recente no arquivo público deste repositório; sem internet, abre a versão embutida no APK. Rascunhos, favoritos e histórico ficam no armazenamento local do aparelho.

## Recursos

- Catálogo de códigos dos blocos A a Q, busca, seleção múltipla, favoritos e códigos recentes.
- Dados de OS, equipamentos, testes, checklist e adversidades de campo.
- Prévia editável, cópia e compartilhamento do texto final.
- Histórico local com pesquisa e exportação CSV; rascunho salvo automaticamente e opção para recuperar.
- Tema claro, escuro ou automático e funcionamento offline após abrir a versão publicada ao menos uma vez.

## Atualizações da interface

O APK consulta `app/src/main/assets/index.html` na branch `main` ao abrir, sem cache, e usa o HTML local se a consulta falhar. Assim, alterações de telas, catálogo e regras JavaScript aparecem no app instalado após publicação no GitHub, sem reinstalação. Mudanças nativas do Android (permissões, integração com o sistema ou código Java) exigem compilar e instalar um APK novo.

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

