import pandas as pd

# Load the CSV file into a DataFrame
df = pd.read_csv('./data/smartphone_dataset_1M.csv')
pd.set_option('display.max_columns', None)

# View the first 5 rows
print(df.head())
df.columns.tolist()