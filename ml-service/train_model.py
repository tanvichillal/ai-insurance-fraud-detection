import numpy as np
import pandas as pd 
from sklearn.model_selection import train_test_split 
from sklearn.linear_model import LinearRegression 
from sklearn.metrics  import mean_squared_error , r2_score , accuracy_score
from sklearn.ensemble import RandomForestClassifier 
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import classification_report
import pickle 


def train(): 
  print("Loading the dataset")
  df = pd.read_csv("fraud_oracle.csv")

  print(df.shape)
  print(df.head())

  df['FraudFound_P'].value_counts()

  drop_cols = ['PolicyNumber'  , 'RepNumber']
  df = df.drop(columns = drop_cols)

  label_encoders = {}
  for col in df.select_dtypes(include='object').columns:
    le = LabelEncoder()
    df[col]  = le.fit_transform(df[col].astype(str))
    label_encoders[col] = le

  X = df.drop(columns=['FraudFound_P'])
  y = df['FraudFound_P']

  print("\nFeature columns:", X.columns.tolist())

  X_train , X_test , y_train , y_test = train_test_split(X , y , test_size = 0.2 , random_state = 42)

  model = RandomForestClassifier(n_estimators=100,max_depth=10,random_state=42)

  model.fit(X_train, y_train)

  y_pred = model.predict(X_test)
  accuracy = accuracy_score(y_test, y_pred)
  print(f"\nModel Accuracy: {accuracy * 100:.2f}%")
  print("\nClassification Report:")
  print(classification_report(y_test, y_pred))

  print("\nSaving model...")
  pickle.dump(model, open("fraud_model.pkl", "wb"))
  pickle.dump(X.columns.tolist(), open("feature_columns.pkl", "wb"))
  pickle.dump(label_encoders, open("label_encoders.pkl", "wb"))

  print("✅ Model saved as fraud_model.pkl")
  print("✅ Feature columns saved as feature_columns.pkl")
  print("✅ Label encoders saved as label_encoders.pkl")

if __name__ == "__main__":
    train()

