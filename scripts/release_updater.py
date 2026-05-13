import os
import re
import json

def main():
    # 1. Parse app/build.gradle.kts to extract version code and name
    build_gradle_path = os.path.join("app", "build.gradle.kts")
    if not os.path.exists(build_gradle_path):
        print(f"Error: {build_gradle_path} not found!")
        return

    with open(build_gradle_path, "r", encoding="utf-8") as f:
        content = f.read()

    # Search for versionCode and versionName patterns
    version_code_match = re.search(r"versionCode\s*=\s*(\d+)", content)
    version_name_match = re.search(r'versionName\s*=\s*"([^"]+)"', content)

    if not version_code_match or not version_name_match:
        print("Error: Could not parse versionCode or versionName from build.gradle.kts")
        return

    version_code = int(version_code_match.group(1))
    version_name = version_name_match.group(1)

    print(f"Extracted Version Code: {version_code}")
    print(f"Extracted Version Name: {version_name}")

    # Set GITHUB_ENV if running in GitHub Actions
    if "GITHUB_ENV" in os.environ:
        with open(os.environ["GITHUB_ENV"], "a", encoding="utf-8") as env_file:
            env_file.write(f"VERSION_CODE={version_code}\n")
            env_file.write(f"VERSION_NAME={version_name}\n")

    # 2. Authenticate and update Firebase Firestore
    service_account_str = os.environ.get("FIREBASE_SERVICE_ACCOUNT")
    if not service_account_str:
        print("Skip: FIREBASE_SERVICE_ACCOUNT environment variable is not set. Firestore will not be updated.")
        return

    try:
        import firebase_admin
        from firebase_admin import credentials, firestore
    except ImportError:
        print("Error: firebase-admin Python library is not installed.")
        return

    # Write temporary credentials file
    cred_file_path = "service_account_credentials.json"
    with open(cred_file_path, "w", encoding="utf-8") as cred_file:
        cred_file.write(service_account_str)

    try:
        # Initialize Firebase Admin SDK
        cred = credentials.Certificate(cred_file_path)
        firebase_admin.initialize_app(cred)
        db = firestore.client()

        repo_name = os.environ.get("GITHUB_REPOSITORY", "Namith-kp/Kreeda-Ankana")
        update_url = f"https://github.com/{repo_name}/releases/latest"

        doc_ref = db.collection("app_settings").document("version_config")
        doc_ref.set({
            "latest_version_code": version_code,
            "latest_version_name": version_name,
            "force_update": False,
            "update_url": update_url
        }, merge=True)

        print("SUCCESS: Firestore updated successfully with the new version configuration!")
    except Exception as e:
        print(f"Error during Firestore update: {e}")
    finally:
        # Secure cleanup of credential file
        if os.path.exists(cred_file_path):
            os.remove(cred_file_path)

if __name__ == "__main__":
    main()
