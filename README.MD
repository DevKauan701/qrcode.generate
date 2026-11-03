# QR Code Generator

API REST em **Java + Spring Boot** que gera QR codes a partir de um texto ou link, salva a imagem em um **storage compatível com S3** e devolve uma URL para acessá-la.

Em desenvolvimento, o storage é o **[RustFS](https://rustfs.com)** rodando no **Docker**, o que dispensa conta e custo na AWS. Como a aplicação usa o AWS SDK, trocar para o S3 real é só mudar a configuração.

## Tecnologias

- Java 21
- Spring Boot 4.1.2 (Spring Web MVC)
- [ZXing](https://github.com/zxing/zxing) 3.5.4: geração do QR code
- AWS SDK for Java v2 2.24.12: comunicação com o storage via API S3
- RustFS: storage S3-compatível, executado com Docker Compose
- Maven

## Como funciona

1. O cliente envia um `POST /qrcode` com o texto desejado.
2. A aplicação gera uma imagem PNG de 300x300 com o QR code.
3. A imagem é enviada para o bucket com um nome único (`UUID.png`).
4. A API devolve uma **URL temporária assinada** para abrir a imagem.

O bucket continua privado: o acesso é feito por uma URL assinada, que expira depois do tempo configurado (7 dias por padrão).

## Arquitetura

O projeto separa a lógica do QR code do armazenamento por meio de uma interface (`StoragePort`). A implementação atual usa S3, mas outra pode ser criada sem alterar o resto do código.

```
src/main/java/com/devkauan701/qrcode/generator/
├── Application.java
├── controller/
│   └── QrCodeController.java        # endpoint REST
├── service/
│   └── QrCodeGeneratorService.java  # gera o QR code e chama o storage
├── ports/
│   └── StoragePort.java             # interface de armazenamento
├── infrastructure/
│   └── S3StorageAdapter.java        # implementação com AWS SDK (S3 / RustFS)
└── dto/
    ├── QrCodeGenerateRequest.java
    └── QrCodeGenerateResponse.java
```

## Pré-requisitos

- [Java 21](https://adoptium.net/)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (ou Docker Engine + Compose)

Confira se estão instalados:

```bash
java -version
docker --version
docker compose version
```

## Como rodar

### 1. Clone o repositório

```bash
git clone <https://github.com/DevKauan701/qrcode.generate>
cd qrcode.generator
```

### 2. Crie o arquivo `.env`

Na raiz do projeto, defina as credenciais do storage. Escolha os valores que quiser:

```env
RUSTFS_ACCESS_KEY=qrcode-admin
RUSTFS_SECRET_KEY=troque-esta-senha
```

> O `.env` não deve ser enviado ao Git. Mantenha-o no `.gitignore`.

### 3. Suba o RustFS

Com o Docker Desktop aberto e funcionando:

```bash
docker compose up -d
docker compose ps
```

O serviço `rustfs` deve aparecer como **Up**. Ele expõe duas portas:

| Porta | Uso |
|-------|-----|
| `9000` | API S3 (usada pela aplicação Java) |
| `9001` | Painel web de administração |

### 4. Crie o bucket

1. Abra o painel em **http://localhost:9001**.
2. Entre com a `RUSTFS_ACCESS_KEY` e a `RUSTFS_SECRET_KEY` do `.env`. Se a tela pedir o endereço do servidor, informe `http://localhost:9000`.
3. Crie um bucket chamado **`qrcode`** (o nome definido em `storage.bucket`).

### 5. Configure a aplicação

Os valores ficam em `src/main/resources/application.properties`:

```properties
storage.endpoint=${STORAGE_ENDPOINT:http://localhost:9000}
storage.public-endpoint=${STORAGE_PUBLIC_ENDPOINT:http://localhost:9000}
storage.region=us-east-1
storage.access-key=${STORAGE_ACCESS_KEY:qrcode-admin}
storage.secret-key=${STORAGE_SECRET_KEY:troque-esta-senha}
storage.bucket=qrcode
storage.url-expiration-minutes=10080
```

| Propriedade | Descrição |
|-------------|-----------|
| `storage.endpoint` | Endereço que a aplicação usa para falar com o storage |
| `storage.public-endpoint` | Endereço que o navegador usa para abrir a imagem (entra na URL gerada) |
| `storage.region` | Região usada na assinatura (`us-east-1` serve para o RustFS) |
| `storage.access-key` / `storage.secret-key` | Credenciais do storage; devem ser iguais às do `.env` |
| `storage.bucket` | Nome do bucket |
| `storage.url-expiration-minutes` | Validade da URL assinada, em minutos |

Cada valor pode ser sobrescrito por variável de ambiente (`STORAGE_ENDPOINT`, `STORAGE_ACCESS_KEY` etc.). Se você mudou as credenciais no `.env`, use os mesmos valores aqui.

### 6. Inicie a aplicação

```bash
./mvnw spring-boot:run
```

No Windows (PowerShell):

```powershell
./mvnw.cmd spring-boot:run
```

A API fica em **http://localhost:8080**.

## Como usar

### `POST /qrcode`

**Corpo da requisição**

```json
{
  "text": "https://github.com"
}
```

**Resposta (200 OK)**

```json
{
  "url": "http://localhost:9000/qrcode/3f1c...e2.png?X-Amz-Algorithm=...&X-Amz-Signature=..."
}
```

Abra a URL completa no navegador para ver o QR code.

**Respostas de erro**

| Status | Quando |
|--------|--------|
| `400` | `text` vazio ou ausente, ou JSON inválido |
| `500` | Falha ao gerar ou enviar a imagem (o detalhe aparece no log da aplicação) |

### Exemplos

**Linux / macOS / Git Bash**

```bash
curl -X POST http://localhost:8080/qrcode \
  -H "Content-Type: application/json" \
  -d '{"text":"https://github.com"}'
```

**PowerShell**

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/qrcode `
  -ContentType "application/json" `
  -Body '{"text":"https://github.com"}'
```

Para copiar a URL inteira para a área de transferência (o PowerShell corta textos longos na tabela):

```powershell
(Invoke-RestMethod -Method Post -Uri http://localhost:8080/qrcode -ContentType "application/json" -Body '{"text":"https://github.com"}').url | Set-Clipboard
```

> No PowerShell, não use `curl` para enviar JSON com aspas duplas: as aspas são alteradas e o servidor recusa o corpo. Use `Invoke-RestMethod`, ou `curl.exe` com o corpo em um arquivo.

## Usando o S3 da AWS

O código não muda. Altere só as propriedades para apontar para a AWS:

```properties
storage.endpoint=https://s3.us-east-1.amazonaws.com
storage.public-endpoint=https://s3.us-east-1.amazonaws.com
storage.region=us-east-1
storage.access-key=<sua-access-key>
storage.secret-key=<sua-secret-key>
storage.bucket=<nome-do-seu-bucket>
```

Use variáveis de ambiente para as credenciais reais e não as coloque no repositório.

## Problemas comuns

| Sintoma | O que verificar |
|---------|-----------------|
| `failed to connect to the docker API` | O Docker Desktop não está aberto. Abra e espere o motor iniciar. |
| Painel (`:9001`) não abre | Rode `docker compose ps`. O `rustfs` precisa estar **Up**. |
| `Connection refused` ao chamar a API | A aplicação Java não está rodando na porta 8080. |
| `NoSuchBucket` | O bucket do `storage.bucket` não foi criado no painel. |
| `InvalidAccessKeyId` / `SignatureDoesNotMatch` / `403` | Credenciais do `application.properties` diferentes das do `.env`. |
| `AccessDenied` ao abrir a imagem | Use a URL **completa**, com tudo o que vem depois do `?`. O bucket é privado e exige a assinatura. |
| `500` sem mensagem na resposta | Veja o stack trace no console da aplicação. |

## Parar o ambiente

```bash
docker compose down        # para os containers e mantém os arquivos
docker compose down -v     # para e apaga também os arquivos do storage
```

## Próximos passos

- Containerizar a aplicação (Dockerfile) e incluí-la no `docker-compose.yml`
- Permitir tamanho e cor do QR code via parâmetros
- Testes automatizados para o serviço e o adaptador de storage

## Autor

**devkauan701**
