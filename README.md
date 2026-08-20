# 🏃‍♂️ RunBuddy  
실시간 러닝 트래킹 & 기록 관리 안드로이드 앱

RunBuddy는 러닝 중 **거리, 페이스, 평균 페이스, 시간, 칼로리, 고도**를 실시간으로 측정하고  
러닝 종료 후 **지도 기반 경로 시각화 및 기록 저장**까지 지원하는 안드로이드 러닝 트래킹 앱입니다.

---

## 📱 주요 기능

### ✅ 실시간 러닝 트래킹
- GPS 기반 실시간 위치 추적
- 총 이동 거리(km) 자동 계산
- 실시간 페이스(min/km) 표시
- 평균 페이스 자동 계산
- Chronometer 기반 운동 시간 측정
- **MET 공식 기반 칼로리 실시간 계산**
- **GPS 고도 정보 실시간 표시**

---

### 🔥 칼로리 계산
- MET(Metabolic Equivalent of Task) 기반 추정 방식
- 사용자 체중(기본값 70kg) 기준 계산
- 시간 경과에 따라 실시간 누적

**사용 공식**
kcal = MET × 3.5 × 체중(kg) / 200 × 운동시간(분)

---

### ⛰ 고도 측정
- GPS `Location.altitude` 값 사용
- 비정상 값 필터링 (−200m ~ 5000m)
- 실시간 고도(m) 표시
- 러닝 기록 및 러닝 카드 이미지에 고도 정보 포함

---

### 🗺 러닝 경로 시각화
- Google Maps SDK 기반 지도 표시
- 러닝 경로를 Polyline으로 시각화
- 전체 경로 기준 자동 카메라 이동

---

### 🧾 러닝 기록 저장
- 러닝 종료 시 기록 자동 저장
- 요일 + 시간대 기반 러닝 제목 자동 생성

**저장 항목**
- 날짜
- 제목
- 총 거리
- 평균 페이스
- 운동 시간
- 칼로리
- 고도
- GPS 경로 좌표

---

### 📊 기록 조회
- 주 / 월 / 전체 기준 기록 필터링
- 기간별 총 거리 자동 합산
- RecyclerView 기반 기록 리스트
- 기록 클릭 시 상세 화면 이동

---

### 🖼 러닝 카드 이미지 생성
- 러닝 기록 정보를 이미지 위에 오버레이
- 표시 정보:
  - 평균 페이스
  - 운동 시간
  - 칼로리
  - 고도
- 갤러리에 이미지 저장 가능

---

### 🔊 음성 안내
- TextToSpeech(TTS) 사용
- 러닝 시작 / 종료 음성 안내 제공

---

## 🧩 프로젝트 구조

com.example.runbuddy
│
├─ ui
│ ├─ running
│ │ └─ RunningActivity.kt
│ ├─ record
│ │ ├─ RecordActivity.kt
│ │ ├─ RecordDetailActivity.kt
│ │ └─ RecordListAdapter.kt
│ └─ map
│ └─ MapActivity.kt
│
├─ data
│ ├─ running
│ │ └─ RunningRepository.kt
│ └─ record
│ ├─ Record.kt
│ └─ RecordRepository.kt
│
├─ service
│ └─ RunningService.kt
│
└─ res
├─ layout
├─ drawable
└─ values


---

## ⚙️ 사용 기술 스택

| 구분 | 기술 |
|---|---|
| 언어 | Kotlin |
| UI | XML Layout |
| 지도 | Google Maps SDK |
| 위치 | FusedLocationProviderClient |
| 백그라운드 | Foreground Service |
| 아키텍처 | Repository 패턴 |
| 상태 관리 | LiveData |
| 데이터 저장 | SharedPreferences |
| 음성 안내 | TextToSpeech |
| 이미지 처리 | Canvas / Bitmap |
| 권한 관리 | Runtime Permission |

---

## 🧠 핵심 로직 설명

### 📐 거리 계산
- Haversine 공식 사용
- GPS 튐 방지 로직 적용
  - 1m 이하 이동 무시
  - 30m 이상 이동 무시

---

### ⏱ 페이스 계산
- 최근 GPS 위치 10개 기준 계산
- 10초 기준 min/km 페이스 산출
- 비정상 페이스 값 필터링

---

### 🔥 칼로리 계산
- MET 기반 실시간 누적 계산
- Chronometer tick 기준 업데이트

---

### ⛰ 고도 처리
- GPS altitude 실시간 반영
- 비정상 값 필터링 후 UI 표시

---

## 🚀 실행 방법

1. Android Studio에서 프로젝트 열기
2. `local.properties.example`을 참고해 `local.properties`에 `MAPS_API_KEY`, `DIRECTIONS_API_KEY` 입력 (본인 Google Cloud 콘솔에서 발급)
3. `app/google-services.json.example`을 참고해 Firebase 콘솔에서 발급받은 `app/google-services.json` 추가
4. 위치 권한 허용
5. 실제 기기에서 실행 (GPS 필수)

> 보안을 위해 API 키와 `google-services.json`은 저장소에 포함되어 있지 않습니다.

---

## 🔐 필수 권한

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>

## 📌 향후 개선 계획

- 사용자 체중 설정 UI 제공
- 러닝 통계 그래프 시각화
- 목표 거리 / 목표 시간 설정 기능
- 클라우드 기반 러닝 기록 동기화
- Wear OS 연동 지원

---

## 👨‍💻 개발자

- 이주영  
- 김준하  
- 서석현  
