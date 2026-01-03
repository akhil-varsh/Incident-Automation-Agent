from flask import Flask, jsonify
from config import Config
from app.extensions import db, migrate, init_celery

def create_app(config_class=Config):
    app = Flask(__name__)
    app.config.from_object(config_class)

    # Initialize extensions
    db.init_app(app)
    migrate.init_app(app, db)
    init_celery(app)

    # Register Blueprints
    from app.api.incidents import bp as incidents_bp
    from app.api.voice import bp as voice_bp
    from app.api.health import bp as health_bp
    
    app.register_blueprint(incidents_bp)
    app.register_blueprint(voice_bp)
    app.register_blueprint(health_bp)

    # Global Error Handler
    @app.errorhandler(Exception)
    def handle_exception(e):
        # Allow pass-through for debug
        if app.debug:
            logger.error(f"Unhandled exception: {e}", exc_info=True)
        
        response = {
            "error": str(e),
            "type": type(e).__name__
        }
        return jsonify(response), 500

    @app.route('/health')
    def health_check():
        return jsonify({"status": "UP", "service": "Incident Automation Agent"})

    return app
