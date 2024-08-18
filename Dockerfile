# 사용할 도커 이미지 설정
FROM openjdk:17-slim

# 필수 도구를 apt-get을 통해 설치
USER root
RUN apt-get update && apt-get install -y default-mysql-client
# 추후 mysql 설정을 하면서 위에 추가함

# JAR 파일 위치에 대한 인수를 설정
ARG JAR_FILE=build/libs/homelearn-0.0.1-SNAPSHOT.jar

# 변수에 지정된 경로에 있는 JAR 파일을 Docker 이미지의 루트 디렉토리에 app.jar라는 이름으로 복사
COPY ${JAR_FILE} app.jar

# 컨테이너가 시작될 때 복사된 jar 파일을 실행할 명령어를 정의
ENTRYPOINT ["java", "-jar", "/app.jar"]