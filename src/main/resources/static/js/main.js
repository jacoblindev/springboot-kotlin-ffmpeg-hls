const audio = document.getElementById('audio');

function uploadMusic(e) {
            e.preventDefault();

            let musicName = e.target.musicName.value;
            let musicAuthor = e.target.musicAuthor.value;
            let musicType = e.target.musicType.value;
            let musicDetail = e.target.musicDetail.value;
            let musicFile = e.target.musicFile.files;

            let formData = new FormData();
            formData.append("musicName", musicName);
            formData.append("musicAuthor", musicAuthor);
            formData.append("musicType", musicType);
            formData.append("musicDetail", musicDetail);
            formData.append("musicFile", musicFile[0]);

            fetch('/audio/upload', {
                method: 'POST',
                body: formData
            })
            .then(async (res) => {
                const data = await res.json();
                if (!res.ok) {
                    // Here is to throw HTTP error (4xx & 5xx)
                    throw (data && data.message) || res.status;
                  }
                  return data;
            })
            .then(data => {
                console.log(data);
                document.getElementById('name-music').innerHTML = data.musicName;
                document.getElementById('author-music').innerHTML = data.musicAuthor;
                document.getElementById('type-music').innerHTML = data.musicType;
                document.getElementById('detail-music').innerHTML = data.musicDetail;
                document.getElementById('url-music').innerHTML = data.musicUrl;
                document.getElementById('url-music').setAttribute('href', data.musicUrl);

                if (audio.canPlayType('application/vnd.apple.mpegurl')) {
                    audio.setAttribute('src', data.musicUrl);
                } else if (Hls.isSupported()) {
                   var hls = new Hls();
                   hls.loadSource(data.musicUrl.split(":8888")[1]);
                   hls.attachMedia(audio);

               }
            })
            .catch(err => {
            	alert("上傳失敗，詳情檢視控制檯");
                throw err
            })
        }