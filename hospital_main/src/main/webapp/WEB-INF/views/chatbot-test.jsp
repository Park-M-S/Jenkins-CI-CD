<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isELIgnored="false" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>챗봇 테스트</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            padding: 20px;
        }

        .container {
            background: white;
            border-radius: 20px;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
            width: 100%;
            max-width: 800px;
            padding: 40px;
        }

        h1 {
            color: #667eea;
            text-align: center;
            margin-bottom: 10px;
            font-size: 32px;
        }

        .subtitle {
            text-align: center;
            color: #666;
            margin-bottom: 30px;
            font-size: 14px;
        }

        .chat-box {
            background: #f8f9fa;
            border-radius: 15px;
            padding: 20px;
            height: 400px;
            overflow-y: auto;
            margin-bottom: 20px;
            border: 2px solid #e0e0e0;
        }

        .message {
            margin-bottom: 15px;
            padding: 12px 18px;
            border-radius: 15px;
            max-width: 80%;
            animation: fadeIn 0.3s;
        }

        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(10px); }
            to { opacity: 1; transform: translateY(0); }
        }

        .user-message {
            background: #667eea;
            color: white;
            margin-left: auto;
            text-align: right;
        }

        .ai-message {
            background: white;
            border: 2px solid #e0e0e0;
            color: #333;
        }

        .ai-message .type-badge {
            display: inline-block;
            padding: 3px 8px;
            border-radius: 5px;
            font-size: 11px;
            font-weight: bold;
            margin-bottom: 8px;
        }

        .type-question { background: #ffd93d; color: #000; }
        .type-recommendation { background: #6bcf7f; color: white; }
        .type-multiple { background: #ff6b6b; color: white; }
        .type-emergency { background: #ff0000; color: white; }

        .questions-list {
            margin-top: 10px;
            padding-left: 20px;
        }

        .questions-list li {
            margin: 5px 0;
            color: #555;
        }

        .input-area {
            display: flex;
            gap: 10px;
        }

        #userInput {
            flex: 1;
            padding: 15px 20px;
            border: 2px solid #e0e0e0;
            border-radius: 25px;
            font-size: 16px;
            outline: none;
            transition: border-color 0.3s;
        }

        #userInput:focus {
            border-color: #667eea;
        }

        button {
            padding: 15px 30px;
            background: #667eea;
            color: white;
            border: none;
            border-radius: 25px;
            font-size: 16px;
            font-weight: bold;
            cursor: pointer;
            transition: all 0.3s;
        }

        button:hover {
            background: #5568d3;
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
        }

        button:disabled {
            background: #ccc;
            cursor: not-allowed;
            transform: none;
        }

        .loading {
            display: none;
            text-align: center;
            color: #667eea;
            font-weight: bold;
            margin: 10px 0;
        }

        .loading.active {
            display: block;
        }

        .clear-btn {
            background: #ff6b6b;
            padding: 10px 20px;
            font-size: 14px;
            margin-top: 10px;
        }

        .clear-btn:hover {
            background: #ee5a52;
        }

        .json-view {
            background: #1e1e1e;
            color: #d4d4d4;
            padding: 15px;
            border-radius: 10px;
            margin-top: 10px;
            font-family: 'Courier New', monospace;
            font-size: 12px;
            max-height: 200px;
            overflow-y: auto;
            display: none;
        }

        .json-view.active {
            display: block;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>🏥 병원 챗봇 테스트</h1>
        <p class="subtitle">증상을 입력하면 AI가 적절한 진료과를 추천해드립니다</p>
        
        <div class="chat-box" id="chatBox">
            <div class="message ai-message">
                <strong>AI 챗봇</strong><br>
                안녕하세요! 어떤 증상이 있으신가요? 편하게 말씀해주세요.
            </div>
        </div>

        <div class="loading" id="loading">AI가 생각하는 중...</div>

        <div class="input-area">
            <input type="text" id="userInput" placeholder="예: 배가 아파요, 발목을 삐었어요" 
                   onkeypress="if(event.key==='Enter') sendMessage()">
            <button onclick="sendMessage()" id="sendBtn">전송</button>
        </div>

        <button class="clear-btn" onclick="clearChat()">대화 초기화</button>

        <div class="json-view" id="jsonView"></div>
    </div>

    <script>
        const API_URL = '/api/chatbot/chat';
        let conversationHistory = [];

        function sendMessage() {
            const input = document.getElementById('userInput');
            const message = input.value.trim();
            
            if (!message) {
                alert('메시지를 입력해주세요!');
                return;
            }

            // 사용자 메시지 표시
            addUserMessage(message);
            input.value = '';
            
            // 로딩 표시
            showLoading(true);
            disableInput(true);

            // API 호출
            fetch(API_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ message: message })
            })
            .then(response => response.json())
            .then(data => {
                showLoading(false);
                disableInput(false);
                addAIMessage(data);
                showJSON(data);
                conversationHistory.push({ user: message, ai: data });
            })
            .catch(error => {
                showLoading(false);
                disableInput(false);
                addErrorMessage('오류가 발생했습니다: ' + error.message);
                console.error('Error:', error);
            });
        }

        function addUserMessage(message) {
            const chatBox = document.getElementById('chatBox');
            const messageDiv = document.createElement('div');
            messageDiv.className = 'message user-message';
            messageDiv.innerHTML = '<strong>나</strong><br>' + escapeHtml(message);
            chatBox.appendChild(messageDiv);
            chatBox.scrollTop = chatBox.scrollHeight;
        }

        function addAIMessage(data) {
            const chatBox = document.getElementById('chatBox');
            const messageDiv = document.createElement('div');
            messageDiv.className = 'message ai-message';
            
            let content = '<strong>AI 챗봇</strong><br>';
            
            // 타입 배지
            if (data.type) {
                content += '<span class="type-badge type-' + data.type + '">' + getTypeLabel(data.type) + '</span><br>';
            }
            
            // 메시지
            content += escapeHtml(data.message || '응답 없음');
            
            // 추천 진료과
            if (data.department) {
                content += '<br><br><strong>📍 추천 진료과:</strong> ' + escapeHtml(data.department);
            }
            
            // 확신도
            if (data.confidence) {
                content += ' (확신도: ' + escapeHtml(data.confidence) + ')';
            }
            
            // 추가 질문들
            if (data.questions && data.questions.length > 0) {
                content += '<br><br><strong>추가 질문:</strong><ul class="questions-list">';
                data.questions.forEach(function(q) {
                    content += '<li>' + escapeHtml(q) + '</li>';
                });
                content += '</ul>';
            }
            
            // 여러 진료과
            if (data.departments && data.departments.length > 0) {
                content += '<br><br><strong>가능한 진료과:</strong> ' + data.departments.map(escapeHtml).join(', ');
            }
            
            messageDiv.innerHTML = content;
            chatBox.appendChild(messageDiv);
            chatBox.scrollTop = chatBox.scrollHeight;
        }

        function addErrorMessage(message) {
            const chatBox = document.getElementById('chatBox');
            const messageDiv = document.createElement('div');
            messageDiv.className = 'message ai-message';
            messageDiv.innerHTML = '<strong>⚠️ 오류</strong><br>' + escapeHtml(message);
            chatBox.appendChild(messageDiv);
            chatBox.scrollTop = chatBox.scrollHeight;
        }

        function showJSON(data) {
            const jsonView = document.getElementById('jsonView');
            jsonView.textContent = JSON.stringify(data, null, 2);
            jsonView.classList.add('active');
        }

        function getTypeLabel(type) {
            const labels = {
                'question': '추가 질문',
                'recommendation': '진료과 추천',
                'multiple': '복수 선택',
                'emergency': '응급 상황',
                'inappropriate': '부적절한 입력',
                'error': '오류'
            };
            return labels[type] || type;
        }

        function showLoading(show) {
            document.getElementById('loading').classList.toggle('active', show);
        }

        function disableInput(disabled) {
            document.getElementById('userInput').disabled = disabled;
            document.getElementById('sendBtn').disabled = disabled;
        }

        function clearChat() {
            const chatBox = document.getElementById('chatBox');
            chatBox.innerHTML = '\
                <div class="message ai-message">\
                    <strong>AI 챗봇</strong><br>\
                    안녕하세요! 어떤 증상이 있으신가요? 편하게 말씀해주세요.\
                </div>\
            ';
            document.getElementById('jsonView').classList.remove('active');
            conversationHistory = [];
        }

        function escapeHtml(text) {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        }

        // Enter 키로 전송
        document.getElementById('userInput').focus();
    </script>
</body>
</html>