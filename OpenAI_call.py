import openai
import os
!pip install openai == 0.28
from dotenv import load_dotenv

load_dotenv()

openai.api_key = os.getenv("OPENAI_API_KEY")

# 2. GPT 모델 호출
def generate_text(prompt, model="gpt-3.5-turbo"): #모델 호출하여 응답 생성
    response = openai.ChatCompletion.create(
        model=model,
        messages=[
            {"role": "system", "content": "You are a helpful assistant."},
            {"role": "user", "content": prompt}
        ], #대화 내용
        max_tokens=150, #생성할 최대 토큰 수
        temperature=0.7 #응답의 창의성 수준
    )
    return response['choices'][0]['message']['content'].strip()

# 2. 키워드 추출 함수
def extract_keywords_from_file(file_path):
    """
    .txt 파일에서 메시지를 읽고 GPT 모델을 통해 키워드를 추출하는 함수.
    """
    # 파일에서 메시지 읽기
    try:
        with open(file_path, "r", encoding="utf-8") as file:
            message = file.read().strip()
    except FileNotFoundError:
        return {"error": "파일을 찾을 수 없습니다. 경로를 확인해주세요."}

    # GPT를 이용해 키워드 추출
    prompt = f"제공된 텍스트를 참고하여 🐥의 관심사 5개를 추출해줘.:\n\n{message}\n\nKeywords:"
    keywords_text = generate_text(prompt)  # GPT 모델 호출
    keywords = [kw.strip() for kw in keywords_text.split(",")]  # 키워드 리스트로 변환

    # 키워드와 원문 반환
    return {"keywords": keywords}

    def recommend_gift(keywords): 
    """
    GPT에게 각 키워드에 맞는 선물을 추천받는 함수.
    """
    recommendations = []

    for keyword in keywords:
        # GPT에게 각 키워드에 맞는 선물 추천을 요청
        prompt = f"키워드 별로 남자 연인에게 기념일에 주기 좋다고 추천된 선물 두개씩 한글로 출력하고, 설명은 덧붙히지 마: {keyword}"
        gift_suggestion = generate_text(prompt)

        recommendations.append(gift_suggestion)

    return recommendations if recommendations else ["gift card"]

    # 실행 예시
if __name__ == "__main__":
    # .txt 파일 경로
    file_path = "example.txt"  # 경로 수정
  # 사용자 메시지를 담은 txt 파일 경로
    result = extract_keywords_from_file(file_path)

    if "error" in result:
        print(result["error"])  # 오류 메시지 출력
    else:
        # 추출된 키워드 출력
        print("추출된 키워드:")
        print(", ".join(result["keywords"]))

        # 선물 추천
        gift_suggestions = recommend_gift(result["keywords"])

        print("\n추천된 선물:")
        for suggestion in gift_suggestions:
            print(suggestion)

