from flask import Flask, send_file
import io

from interactivity_movie import InteractiveMovie, generate_playlist_string

def create_app():
    app = Flask(__name__)
    movies = {
        'test_movie': InteractiveMovie('data/test_movie/script.yaml')
    }

    @app.route('/stream_movie/<movie_id>/<node_id>/get_chunk/<chunk_name>', methods=['GET'])
    def stream_chunk(movie_id,node_id,chunk_name):
        return send_file('data/%s/%s/%s' % (movie_id, node_id, chunk_name), mimetype = 'video/ts')

    @app.route('/stream_movie/<movie_id>/<node_id>/index.m3u8', methods=['GET'])
    def stream_movie(movie_id, node_id):
        movie = movies[movie_id]
        fragment = movie.fragments[node_id]
        playlist_info = fragment.video_info['720']
        answer = generate_playlist_string(
            fragment.hls_version, fragment.upper_duration_bound, 0,
            False, playlist_info.chunks, 'get_chunk/'
        )

        buffer = io.BytesIO()
        buffer.write(answer.encode('utf-8'))
        buffer.seek(0)
        return send_file(buffer, as_attachment = True, attachment_filename = 'index.m3u8', mimetype = 'text/csv')

    return app
