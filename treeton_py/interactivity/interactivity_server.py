from flask import Flask, send_file, request, send_from_directory
from filelock import FileLock
import io, os, logging

from interactivity_movie import InteractiveMovie, generate_playlist_string

class SessionInfo(object):
    def __init__(self, id, movie):
        self.id = id
        self.movie = movie
        self.previous_fragment = None
        self.previous_discontinuity = False
        self.current_fragment = movie.root
        self.n_skipped_chunks = 0
        self.current_discontinuity = True

    def make_choice(self, option_name):
        if self.previous_fragment:
            self.n_skipped_chunks += self.previous_fragment.number_of_chunks

        self.previous_fragment = self.current_fragment
        self.previous_discontinuity = self.current_discontinuity

        assert self.current_fragment.bifurcation_options
        option_info = self.current_fragment.bifurcation_options[option_name]
        self.current_fragment = option_info[2]
        self.current_discontinuity = option_info[1]

SESSIONS_DIR = 'data/.sessions'
LOCKS_DIR = 'data/.locks'

def create_app():
    app = Flask(__name__)

    gunicorn_logger = logging.getLogger('gunicorn.error')
    app.logger.handlers = gunicorn_logger.handlers
    app.logger.setLevel(gunicorn_logger.level)
    movies = {}

    for root, dirs, files in os.walk('data'):
        for file in files:
            if file.endswith('.yaml'):
                config_path = os.path.join(root, file)
                movies[os.path.basename(os.path.dirname(config_path))] = InteractiveMovie(config_path)
                app.logger.info('Loaded movie from %s' % config_path)

    if not os.path.exists(SESSIONS_DIR):
        os.mkdir(SESSIONS_DIR)

    if not os.path.exists(LOCKS_DIR):
        os.mkdir(LOCKS_DIR)

    for f in os.listdir(SESSIONS_DIR):
        full_path = os.path.join(SESSIONS_DIR, f)
        if os.path.isfile(full_path):
            os.remove(full_path)

    def send_as_attached_playlist(text_answer):
        buffer = io.BytesIO()
        buffer.write(text_answer.encode('utf-8'))
        buffer.seek(0)
        return send_file(buffer, as_attachment = True, attachment_filename = 'index.m3u8', mimetype = 'text/csv')

    def write_session_info(session_info):
        with open(os.path.join(SESSIONS_DIR,session_info.id), 'w+') as target_file:
            strings = [
                session_info.movie.id,
                session_info.previous_fragment.id if session_info.previous_fragment else '-1',
                session_info.current_fragment.id,
                str(session_info.n_skipped_chunks),
                str(session_info.previous_discontinuity),
                str(session_info.current_discontinuity)
            ]
            target_file.write('\n'.join(strings))

    def load_session_info(session_id):
        with open(os.path.join(SESSIONS_DIR, session_id), 'r') as target_file:
            strings = target_file.readlines()
            movie = movies[strings[0].strip()]
            result = SessionInfo(session_id, movie)
            prev_frag_id = strings[1].strip()
            cur_frag_id = strings[2].strip()
            result.previous_fragment = movie.fragments[prev_frag_id] if prev_frag_id != '-1' else None
            result.current_fragment = movie.fragments[cur_frag_id]
            result.n_skipped_chunks = int(strings[3].strip())
            result.previous_discontinuity = True if strings[4].strip() == 'True' else False
            result.current_discontinuity = True if strings[5].strip() == 'True' else False

            return result

    @app.after_request
    def add_header(response):
        response.headers['Cache-Control'] = 'no-cache, no-store'
        return response

    @app.route('/demo/<path>', methods=['GET'])
    def serve_demo(path):
        return send_from_directory('static/demo', path)

    @app.route('/dist/<path>', methods=['GET'])
    def serve_dist(path):
        return send_from_directory('static/dist', path)

    @app.route('/stream_movie/<movie_id>/index.m3u8', methods=['GET'])
    def stream_movie(movie_id):
        session_id = str(len(os.listdir(SESSIONS_DIR)))
        movie = movies[movie_id]
        session_info = SessionInfo(session_id, movie)

        app.logger.info('Generated session id %s' % session_id)

        strings = [
            '#EXTM3U',
            '#EXT-X-PLAYLIST-TYPE:EVENT',
            '#EXT-X-VERSION:4'
            '#EXT-X-STREAM-INF:BANDWIDTH=1280000,AVERAGE-BANDWIDTH=1000000',
            'http://%s/stream_session/%s/360/index.m3u8' % (request.host, session_id),
            '#EXT-X-STREAM-INF:BANDWIDTH=2560000,AVERAGE-BANDWIDTH=2000000',
            'http://%s/stream_session/%s/480/index.m3u8' % (request.host, session_id),
            '#EXT-X-STREAM-INF:BANDWIDTH=7680000,AVERAGE-BANDWIDTH=6000000',
            'http://%s/stream_session/%s/720/index.m3u8' % (request.host, session_id),
            ''
        ]

        write_session_info(session_info)

        return send_as_attached_playlist('\n'.join(strings))

    @app.route('/get_chunk/<movie_id>/<node_id>/<chunk_name>', methods=['GET'])
    def stream_chunk(movie_id, node_id, chunk_name):
        app.logger.info('Chunk %s was requested (movie_id %s, node_id %s)' % (chunk_name, movie_id, node_id))

        return send_file('data/%s/%s/%s' % (movie_id, node_id, chunk_name), mimetype = 'video/ts')

    @app.route('/stream_session/<session_id>/<quality>/index.m3u8', methods=['GET'])
    def stream_session(session_id, quality):
        with FileLock(os.path.join(LOCKS_DIR, session_id + '.lock')):
            app.logger.info('Playlist for session id %s with quality %s was requested' % (session_id, quality))
            session_info = load_session_info(session_id)
            first_chunks = session_info.previous_fragment.video_info[quality].chunks if session_info.previous_fragment else []
            second_chunks = session_info.current_fragment.video_info[quality].chunks

            first_gap = [None] if session_info.previous_discontinuity else []
            second_gap = [None] if session_info.current_discontinuity else []

            all_chunks = first_gap + first_chunks + second_gap + second_chunks

            # TODO придумать когда чистить протухшие сессии

            return send_as_attached_playlist(generate_playlist_string(
                3, session_info.current_fragment.upper_duration_bound, 0,
                False, all_chunks, 'http://%s/get_chunk/' % request.host,
                not session_info.current_fragment.bifurcation_options
            ))

    @app.route('/make_choice/<session_id>/<option_name>', methods=['GET'])
    def make_choice(session_id, option_name):
        with FileLock(os.path.join(LOCKS_DIR, session_id + '.lock')):
            session_info = load_session_info(session_id)
            session_info.make_choice(option_name)
            write_session_info(session_info)
        return 'OK'

    #TODO сделать метод get_script

    return app
