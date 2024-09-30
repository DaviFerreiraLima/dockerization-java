# Mini Curso: Docker, Kubernetes e Pipelines para Desenvolvedores

Este mini curso visa ensinar conceitos básicos de Docker, Kubernetes e a criação de pipelines, com foco em desenvolvedores. Ao longo do curso, iremos abordar o empacotamento de uma aplicação Java, containerização com Docker (incluindo Multi-stage builds), envio de imagens para o DockerHub usando GitHub Actions e, por fim, a criação de um cluster Kubernetes local para a implementação da aplicação.

## Estrutura do Curso

1. **Empacotamento de Aplicações Java com Maven**
    - Vamos criar uma aplicação simples em Java que retorna "Hello World" e empacotá-la usando Maven.
  
2. **Dockerização da Aplicação Java**
    - Explicação sobre Docker e os principais comandos.
    - **Dockerfile com build simples**: Como criar uma imagem Docker a partir da aplicação Java.
    - **Dockerfile Multi-stage build**: Explicação sobre a utilização do Docker Multi-stage build para otimizar o tamanho da imagem.
  
3. **Publicação da Imagem no DockerHub**
    - Criação de um pipeline com GitHub Actions para automatizar o envio da imagem ao DockerHub.

4. **Criação de um Cluster Kubernetes Local**
    - Criação de um cluster Kubernetes local com ferramentas como Minikube ou Kind.
    - **Deployment e Service**: Deploy da aplicação containerizada no Kubernetes e acesso local via Service.

## Aplicação Java de Exemplo

A aplicação Java utilizada neste curso é um simples "Hello World" que responde na porta 8080.

### Estrutura do Projeto

O projeto Java segue a estrutura de um aplicativo Maven básico:



## Dockerização da Aplicação

Aqui estão duas abordagens diferentes para a criação da imagem Docker: uma com **build simples** e outra utilizando o **Multi-stage build**.

### 1. Dockerfile: Build Simples

Neste Dockerfile, a imagem base usada contém o Maven e o JDK 17, permitindo a construção do aplicativo e sua execução dentro do mesmo ambiente:

```dockerfile
# Use uma imagem base com JDK 17 e Maven instalados
FROM maven:3.8.4-openjdk-17

# Define o diretório de trabalho no container
WORKDIR /app

# Copia o arquivo pom.xml
COPY pom.xml .

# Copia o código-fonte
COPY src ./src

# Constrói a aplicação
RUN mvn clean package

# Expõe a porta 8080
EXPOSE 8080

# Comando para rodar a aplicação
CMD ["java", "-jar", "target/*.jar"]

```

### 2. Dockerfile: Build Multi-stage 

O Multi-stage build é uma técnica para otimizar o tamanho final da imagem. Ele separa a construção do código da execução, o que nos permite usar uma imagem base menor (apenas com o JRE) na fase de execução:

```dockerfile
# Imagem base com Maven e JDK 17
FROM maven:3.8.4-openjdk-17-slim AS build

# Define o diretório de trabalho
WORKDIR /app

# Copia o pom.xml e baixa as dependências
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copia o código-fonte
COPY src ./src

# Constrói o aplicativo (pula testes para acelerar)
RUN mvn clean package -DskipTests

# Segunda fase: Usa uma imagem JRE mínima para o ambiente de execução
FROM eclipse-temurin:17-jre-alpine

# Define o diretório de trabalho
WORKDIR /app

# Copia o jar da fase de build
COPY --from=build /app/target/*.jar ./app.jar

# Expõe a porta 8080
EXPOSE 8080

# Executa a aplicação
CMD ["java", "-jar", "app.jar"]
```



## Pipeline com GitHub Actions

A seguir, um exemplo de pipeline que constrói a imagem da aplicação e a envia para o DockerHub automaticamente.

Arquivo .github/workflows/docker-image.yml:

``` yaml
name: Docker Image CI

on:
  push:
    branches:
      - main

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - name: Checkout code
      uses: actions/checkout@v2

    - name: Set up Docker Buildx
      uses: docker/setup-buildx-action@v1

    - name: Log in to DockerHub
      uses: docker/login-action@v1
      with:
        username: ${{ secrets.DOCKERHUB_USERNAME }}
        password: ${{ secrets.DOCKERHUB_TOKEN }}

    - name: Build and push Docker image
      uses: docker/build-push-action@v2
      with:
        context: .
        file: ./Dockerfile
        push: true
        tags: ${{ secrets.DOCKERHUB_USERNAME }}/hello-world-java:latest

```

## Cluster Kubernetes Local

Depois de enviar a imagem para o DockerHub, iremos rodá-la em um cluster Kubernetes local.

### 1. Instalação do Minikube ou Kind

Instruções para instalar o Minikube ou Kind no seu ambiente local:

- [Minikube](https://minikube.sigs.k8s.io/docs/start/)
- [Kind](https://kind.sigs.k8s.io/docs/user/quick-start/)

### 2. Criação do Deployment e Service

Criação dos recursos Kubernetes para rodar a aplicação e acessá-la localmente.

#### Arquivo `deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: hello-world-deployment
spec:
  replicas: 1
  selector:
    matchLabels:
      app: hello-world
  template:
    metadata:
      labels:
        app: hello-world
    spec:
      containers:
      - name: hello-world
        image: <dockerhub-username>/hello-world-java:latest
        ports:
        - containerPort: 8080
```

#### Arquivo `service.yaml`:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: hello-world-service
spec:
  selector:
    app: hello-world
  ports:
    - protocol: TCP
      port: 8080
      targetPort: 8080
  type: NodePort
```

### 3. Aplicar Configurações no Kubernetes

Execute os seguintes comandos para aplicar os arquivos no cluster:

```bash
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
```

### 4. Acessar a Aplicação Localmente

Dependendo do tipo de cluster Kubernetes que você está utilizando (Minikube ou Kind), o acesso à aplicação pode variar.

#### A. Minikube

Se estiver usando **Minikube**, utilize o comando abaixo para expor o serviço e abrir o navegador automaticamente com o IP correto:

```bash
minikube service hello-world-service
```

Isso abrirá a aplicação no navegador no endereço correto.

#### B. Kind

Se estiver usando **Kind**, você pode expor o serviço manualmente utilizando **port-forward**. Execute o seguinte comando para redirecionar a porta 8080 do serviço para a porta 8080 no seu host local:

```bash
kubectl port-forward service/hello-world-service 8080:8080
```

Acesse a aplicação no navegador ou via `curl`:

```bash
http://localhost:8080
```

#### Verificando a Porta do NodePort

Se você deseja verificar qual porta foi exposta pelo Kubernetes, use o seguinte comando para visualizar o mapeamento:

```bash
kubectl get svc hello-world-service
```

A saída mostrará algo como:

```
NAME                  TYPE       CLUSTER-IP      EXTERNAL-IP   PORT(S)          AGE
hello-world-service   NodePort   10.96.54.1      <none>        8080:XXXX/TCP    5m
```

Substitua `XXXX` pela porta que foi exposta e acesse a aplicação no navegador:

```bash
http://<minikube-ip>:XXXX
```

### Conclusão

Neste mini curso, cobrimos o processo completo desde a criação de uma aplicação Java, passando pela sua containerização com Docker, até a implementação em um cluster Kubernetes local. O pipeline de CI/CD com GitHub Actions facilita a automação do processo de build e publicação no DockerHub.

Agora você pode testar sua aplicação localmente utilizando Minikube ou Kind e experimentar o processo de implementação em Kubernetes.
