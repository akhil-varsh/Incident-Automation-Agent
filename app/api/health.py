from flask import Blueprint, jsonify
from datetime import datetime

bp = Blueprint('health', __name__, url_prefix='/api')

@bp.route('/health', methods=['GET'])
def health():
    return jsonify({
        "status": "UP",
        "timestamp": datetime.utcnow().isoformat(),
        "service": "incident-automation-agent-python"
    })
