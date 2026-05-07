# Assinador PDF ICP-Brasil A3 (Java Swing)

Primeira versão funcional de aplicativo desktop local para assinatura digital de PDF usando certificado ICP-Brasil A3 via PKCS#11.

## Requisitos (Windows)

1. Instalar Java 17 (JDK).
2. Instalar Maven 3.9+.
3. Instalar driver/middleware do token/cartão A3.
4. Descobrir caminho da DLL PKCS#11 do driver.
   - SafeNet/eToken: `eTPKCS11.dll`
   - Watchdata: `wdpkcs.dll` (ou similar)
   - Oberthur/Gemalto: depende do driver instalado
   - Certisign/SafeSign: `aetpkss1.dll` (ou similar)

## Build

```bash
mvn clean package
```

## Execução

```bash
java -jar target/assinador-ipc-brasil-1.0.0-SNAPSHOT-all.jar
```

## Fluxo de uso

1. Clique em **Abrir PDF**.
2. Navegue entre páginas com **Página anterior** e **Próxima página**.
3. Selecione com o mouse a área retangular da assinatura.
4. Clique em **Configurar A3** e informe:
   - caminho da DLL PKCS#11;
   - PIN do token/cartão.
5. Clique em **Carregar certificados**.
6. Selecione o certificado no combo.
7. Clique em **Assinar PDF** e escolha o novo arquivo de saída.

## Observações

- O arquivo original não é sobrescrito.
- O PIN não é persistido em arquivo.
- Assinatura criptográfica CMS/PKCS#7 embutida no PDF com PDFBox + Bouncy Castle.

## Modos de assinatura

Antes de assinar, escolha um modo na interface:

- **Permitir outras assinaturas depois** (padrão):
  - assinatura digital normal em modo incremental;
  - preserva assinaturas anteriores;
  - permite assinaturas posteriores no documento.

- **Bloquear documento após esta assinatura**:
  - aplica assinatura de certificação (DocMDP) com permissão restrita;
  - tenta impedir alterações posteriores incompatíveis com a certificação.

### Diferenças conceituais

- **Assinatura normal**: assinatura digital sem certificação de bloqueio.
- **Assinatura que permite assinaturas posteriores**: é a assinatura normal feita de forma incremental, mantendo possibilidade de novas assinaturas.
- **Assinatura de certificação/bloqueio**: assinatura com permissões DocMDP para restringir alterações futuras.

## Teste local no Windows

1. instalar Java 17;
2. instalar Maven;
3. instalar driver do token A3;
4. localizar a DLL PKCS#11;
5. rodar `mvn clean package -DskipTests`;
6. executar o jar;
7. abrir PDF;
8. selecionar área;
9. configurar DLL e PIN;
10. carregar certificado;
11. assinar em modo normal;
12. validar no verificador oficial;
13. repetir com modo bloqueio/certificação.

> Se ocorrer `403 Forbidden` ao acessar o Maven Central, o bloqueio é externo ao código/projeto (ambiente/rede).


## Modo desenvolvedor (compilar e executar localmente)

Este modo é para quem vai desenvolver/compilar o projeto.

Pré-requisitos:
- Java 17 (JDK)
- Maven

Comandos úteis no Windows:
- Build: `scripts\build.bat`
- Execução local: `scripts\run.bat`

## Modo usuário final (sem instalar Java/Maven)

Este modo é para uso do aplicativo como programa normal do Windows.

1. Em uma máquina de build (com Java 17 + Maven), gere o instalador:
   - `scripts\package-windows.bat`
2. O instalador será gerado em `dist\installer` (EXE preferencialmente; MSI como fallback).
3. O pacote inclui runtime Java embutido (jpackage), então o usuário final **não precisa** instalar Java ou Maven.
4. O usuário final ainda precisa instalar o driver/middleware do certificado A3/token/cartão e ter acesso à DLL PKCS#11 do fornecedor.

## Empacotamento Windows com jpackage

O script `scripts\package-windows.bat`:
- valida presença do `jpackage`;
- executa build Maven se necessário;
- gera `app-image` em `dist\image`;
- gera instalador Windows chamado **Assinador ICP-Brasil** em `dist\installer`.


## Distribuição para usuário final (Windows)

> **Importante:** arquivos `.bat` são scripts internos para desenvolvedores/CI (ex.: GitHub Actions). Eles **não** são o formato final de instalação para o usuário.

Para o usuário final, o formato principal é:
- **Instalador**: `Assinador-ICP-Brasil-Setup.exe`

Opcionalmente, também pode ser distribuído:
- **Versão portátil**: `Assinador-ICP-Brasil-Portable.zip` (extrair e executar)

### O que o usuário final precisa

- **Não precisa** instalar Java.
- **Não precisa** instalar Maven.
- **Precisa** instalar o driver/middleware do token/cartão A3 e usar a DLL PKCS#11 do fornecedor.

### Como gerar os artefatos de distribuição (dev/CI)

1. Execute `scripts\package-windows.bat` em Windows com JDK 17 (com `jpackage`) + Maven.
2. O script gera:
   - `dist\installer\Assinador-ICP-Brasil-Setup.exe`
   - `dist\portable\Assinador-ICP-Brasil-Portable.zip`
3. Publique esses arquivos para download do usuário final.

## GitHub Actions (artefatos)

Workflow: `.github/workflows/windows-package.yml`
- compila o projeto;
- executa o empacotamento com `jpackage`;
- publica como artifact:
  - instalador `.exe`;
  - versão portátil `.zip` (quando gerada).


## Regras do modo bloqueio/certificação

- O modo de bloqueio/certificação só pode ser usado na **primeira assinatura** do PDF.
- Se o documento já tiver assinatura anterior, use o modo normal para adicionar assinatura incremental.
- A cadeia de certificados do alias selecionado é incluída no CMS quando disponível no token (`getCertificateChain`).


## Algoritmo de assinatura

- Chave RSA: `SHA256withRSA`.
- Chave EC/ECDSA: `SHA256withECDSA`.
- Outros algoritmos: não suportados nesta versão (erro explícito).
