FROM ubuntu:latest
LABEL authors="manik"

ENTRYPOINT ["top", "-b"]
