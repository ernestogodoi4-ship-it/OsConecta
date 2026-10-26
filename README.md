# OS Conecta para Android

Aplicativo Android para gerar encerramentos de OS de telecomunicações. Ao iniciar, o app busca a interface HTML mais recente no arquivo público deste repositório; sem internet, abre a versão embutida no APK. Rascunhos, favoritos e histórico ficam no armazenamento local do aparelho.

## Recursos

- Catálogo de códigos dos blocos A a Q, busca, seleção múltipla, favoritos e códigos recentes.
- Dados de OS, equipamentos, testes, checklist e adversidades de campo.
- Aba Segurança com checklist de apoio, estados locais por protocolo, alerta de parada e sem inclusão no texto final, histórico ou exportações da OS.
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

## Checklist auxiliar de segurança

A aba **Segurança** contém uma conferência operacional separada do encerramento da OS. Os estados são salvos localmente por número de protocolo e não entram no texto gerado, no histórico ou nas exportações do encerramento. A lista cobre planejamento/APR, deslocamento, ambiente, acesso, trânsito, poste e risco elétrico, altura e resposta a emergência, caixas subterrâneas/escavações quando aplicáveis, EPI, risco óptico e fragmentos de fibra, ferramentas, produtos de limpeza e ergonomia.

O checklist é uma ajuda de memória e não substitui o PGR, a APR/AR, procedimentos internos, treinamento, autorização, definição de EPI ou plano de emergência/resgate. Foi elaborado como ferramenta auxiliar com base nas páginas oficiais do MTE para [NR-01](https://www.gov.br/trabalho-e-emprego/pt-br/acesso-a-informacao/participacao-social/conselhos-e-orgaos-colegiados/comissao-tripartite-paritaria-permanente/normas-regulamentadora/normas-regulamentadoras-vigentes/nr-1), [NR-06](https://www.gov.br/trabalho-e-emprego/pt-br/acesso-a-informacao/participacao-social/conselhos-e-orgaos-colegiados/comissao-tripartite-paritaria-permanente/normas-regulamentadora/normas-regulamentadoras-vigentes/norma-regulamentadora-no-6-nr-6), [NR-10](https://www.gov.br/trabalho-e-emprego/pt-br/acesso-a-informacao/participacao-social/conselhos-e-orgaos-colegiados/comissao-tripartite-paritaria-permanente/normas-regulamentadora/normas-regulamentadoras-vigentes/norma-regulamentadora-no-10-nr-10), [NR-17](https://www.gov.br/trabalho-e-emprego/pt-br/acesso-a-informacao/participacao-social/conselhos-e-orgaos-colegiados/comissao-tripartite-paritaria-permanente/normas-regulamentadora/normas-regulamentadoras-vigentes/norma-regulamentadora-no-17-nr-17), [NR-18](https://www.gov.br/trabalho-e-emprego/pt-br/acesso-a-informacao/participacao-social/conselhos-e-orgaos-colegiados/comissao-tripartite-paritaria-permanente/normas-regulamentadora/normas-regulamentadoras-vigentes/norma-regulamentadora-no-18-nr-18), [NR-33](https://www.gov.br/trabalho-e-emprego/pt-br/acesso-a-informacao/participacao-social/conselhos-e-orgaos-colegiados/comissao-tripartite-paritaria-permanente/normas-regulamentadora/normas-regulamentadoras-vigentes/norma-regulamentadora-no-33-nr-33) e [NR-35](https://www.gov.br/trabalho-e-emprego/pt-br/acesso-a-informacao/participacao-social/conselhos-e-orgaos-colegiados/comissao-tripartite-paritaria-permanente/normas-regulamentadora/normas-regulamentadoras-vigentes/norma-regulamentadora-no-35-nr-35). A aplicação das normas depende da atividade e do risco caracterizado. Consulte versões vigentes e os requisitos da organização.

A Viva Telecom apresenta publicamente serviços de conectividade por fibra e atendimento regional; o site institucional não publica procedimento de segurança de campo, então nenhum procedimento interno foi inferido como política da empresa: https://vivastelecom.com.br/.

## Release e APK

Um push na branch `main` compila o APK debug e cria um GitHub Release com o arquivo anexado, usando a `versionName` do projeto como tag, caso esse release ainda não exista. Para publicar uma nova versão, incremente `versionCode` e `versionName` em `app/build.gradle`. A versão atual do projeto é 1.2.0 (versionCode 3). O APK debug é para instalação e avaliação; distribuição formal deve usar APK assinado pela organização.
