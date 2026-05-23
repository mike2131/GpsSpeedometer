# GpsSpeedometer

POCO X7 (Android) 向けに最適化された、高精度・低遅延のGPSスピードメーターアプリです。

## 概要
120HzリフレッシュレートとOLEDディスプレイ（漆黒のダークモード）に対応し、視認性の高いリアルタイム速度表示と走行軌跡の記録・管理機能を提供します。

## 主な機能
- **リアルタイム速度表示**: 小数点第1位まで表示されるキビキビとした速度計。
- **自動計測**: アプリ起動と同時にGPS計測を自動開始。
- **地図表示**: Google Maps上へのリアルタイムな現在地および軌跡の描画。
- **履歴管理**: カレンダーから過去の走行データを選択し、地図上に再現。
- **GPXエクスポート**: 走行ログをGPX形式で出力し、PC等へ共有。
- **スリープ防止**: 鍵アイコンのトグルにより、計測中の画面消灯を防止。
- **速度補完**: 加速度センサーを用いた、GPSロスト時の速度推定機能。

## 使用方法
1. アプリを起動すると位置情報の許可を求められるので「許可」します。
2. 起動と同時に速度計測が始まります。
3. 画面下部の「PAUSE」で一時停止、「RESUME」で再開、「STOP」で記録を保存します。
4. 鍵アイコンをタップすると、画面が常時点灯（緑色）になります。
5. カレンダーアイコンから日付を選択すると、その日の軌跡を地図で確認・共有できます。

## ファイル構成とサマリー

### Common Main (共通ロジック)
- **App.kt**: メインUI（Scaffold, 速度表示, 各種ボタン）の実装。
- **SpeedViewModel.kt**: UIの状態管理、計測の自動開始、DB保存・読み込みの制御。
- **TripRepository.kt**: SQLiteとの通信、GPX変換、ワープ除去フィルターロジック。
- **SpeedInfo.kt**: リアルタイム走行データのモデル（速度、座標リスト等）。
- **LocationService.kt**: プラットフォーム共通の位置情報インターフェース。
- **MapView.kt (expect)**: プラットフォーム別の地図表示の定義。
- **DatabaseDriverFactory.kt (expect)**: プラットフォーム別のDBドライバー定義。
- **Trip.sq**: SQLDelightによるデータベーススキーマとクエリ定義。

### Android Main (Android専用実装)
- **MainActivity.kt**: アプリの起動、パーミッション要求、ViewModelの初期化。
- **AndroidLocationService.kt**: Fused Location Providerと加速度センサーによる実際の計測処理。
- **MapView.kt (actual)**: Google Maps SDK for Androidを用いた地図表示の具体的な実装。
- **DatabaseDriverFactory.kt (actual)**: Android上でのSQLiteデータベース生成処理。
- **ShareUtils.kt**: FileProviderを使用したGPXファイルの外部共有。
- **AndroidManifest.xml**: 権限、サービス、APIキーの設定。

## 開発環境
- macOS
- Java 17
- Gradle 8.2
- Android SDK 34
- Compose Multiplatform 1.6.1
