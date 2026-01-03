
import sys
import os

# Add current dir to path
sys.path.append(os.getcwd())

try:
    print("Attempting to import app factory...")
    from app import create_app
    print("Import successful.")
    
    print("Attempting to create app instance...")
    # Mocking DB URI if not set to avoid connection error during creation if strictly needed
    # But SqlAlchemy usually doesn't connect until request or explicit call
    os.environ['DATABASE_URL'] = 'sqlite:///:memory:' 
    app = create_app()
    print("App instance created successfully.")
    
except Exception as e:
    print(f"VERIFICATION FAILED: {e}")
    import traceback
    traceback.print_exc()
