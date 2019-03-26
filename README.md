# springboot2-async-chat-demo
SpringBoot2.x系で非同期メッセージングを使ったチャットアプリケーションの作例

内容:

* 2018-12 : Spring MVC で SseEmitter を使った Server-Sent Events のサンプルを作成。
* 2019-03 : Spring MVC で WebSocket API を使ったデモを作成。

## 実行環境

* 2018-12時点でJava8で動作確認しています。

## 開発環境

* AdoptOpenJDK 8 (jdk8u192-b12)
  * https://adoptopenjdk.net/
* Spring Tools 4 for Eclipse (Eclipse IDE 2018-12 R, 4.10系)
  * https://spring.io/tools
* Maven >= 3.5.4 (maven-wrapperにて自動的にDLしてくれる)
* ソースコードやテキストファイル全般の文字コードはUTF-8を使用

## ビルドと実行

jarファイルをビルドして実行し、 http://localhost:18088/ にアクセスしてください。

```
cd springboot2-async-chat-demo/

ビルド:
./mvnw package

jarファイルから実行:
java -jar target/springboot2-async-chat-demo-v20181227.1.jar
```

2018-12時点で以下のブラウザで動作確認しています。

* PC版 Chrome 71
* PC版 Firefox 64

## Eclipseプロジェクト用の設定

https://github.com/SecureSkyTechnology/howto-eclipse-setup の `setup-type2` を使用。README.mdで以下を参照のこと:

* Clean Up/Formatter 設定
* GitでcloneしたMavenプロジェクトのインポート

また、Spring Tools 4 for Eclipse に Lombok をインストールしてください : https://projectlombok.org/
