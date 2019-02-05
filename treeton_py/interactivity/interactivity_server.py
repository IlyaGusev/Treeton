from flask import Flask, send_file
import io

app = Flask(__name__)

@app.route('/')
def hello():
    return "Hello world!"

@app.route('/stream_video/stream_chunk/<chunk_name>', methods=['GET'])
def stream_chunk(chunk_name):
    return send_file(chunk_name, mimetype = 'video/ts')

@app.route('/stream_video/<node_id>.m3u8', methods=['GET'])
def stream_video(node_id):
    answer = '\n'.join([
        '#EXTM3U',
        '#EXT-X-VERSION:3',
        '#EXT-X-TARGETDURATION:18',
        '#EXT-X-MEDIA-SEQUENCE:0',
        '#EXTINF:17.800000,',
        'stream_chunk/index0.ts',
        '#EXTINF:3.320000,',
        'stream_chunk/index1.ts',
        '#EXTINF:10.000000,',
        'stream_chunk/index2.ts',
        '#EXTINF:15.800000,',
        'stream_chunk/index3.ts',
        '#EXTINF:4.760000,',
        'stream_chunk/index4.ts',
        '#EXT-X-ENDLIST',
        ''
    ])
    buffer = io.BytesIO()
    buffer.write(answer.encode('utf-8'))
    buffer.seek(0)
    return send_file(buffer, as_attachment = True, attachment_filename = 'index.m3u8', mimetype = 'text/csv')

if __name__ == '__main__':
    app.run()