export const categories = [
  ['GENERAL', 'General'],
  ['FOOD_AND_DINING', 'Food & Dining'],
  ['GROCERIES', 'Groceries'],
  ['TRANSPORTATION', 'Transportation'],
  ['UTILITIES', 'Utilities'],
  ['RENT', 'Rent'],
  ['HOUSING', 'Housing'],
  ['HEALTHCARE', 'Healthcare'],
  ['MEDICINE', 'Medicine'],
  ['EDUCATION', 'Education'],
  ['ENTERTAINMENT', 'Entertainment'],
  ['SHOPPING', 'Shopping'],
  ['TRAVEL', 'Travel'],
  ['SUBSCRIPTIONS', 'Subscriptions'],
  ['PERSONAL_CARE', 'Personal Care'],
  ['FAMILY', 'Family'],
  ['GIFTS_AND_DONATIONS', 'Gifts & Donations'],
  ['SAVINGS', 'Savings'],
  ['DEBT_PAYMENT', 'Debt Payment'],
  ['INSURANCE', 'Insurance'],
  ['TAXES', 'Taxes'],
  ['SALARY', 'Salary'],
  ['ALLOWANCE', 'Allowance'],
  ['BUSINESS', 'Business'],
  ['OTHER', 'Other']
];

export function populateCategorySelect(select, includeBlank = false) {
  select.innerHTML = [
    ...(includeBlank ? [['', 'General']] : []),
    ...categories
  ].map(([value, label]) => `<option value="${value}">${label}</option>`).join('');
}

export function categoryLabel(value) {
  return categories.find(([key]) => key === value)?.[1] || value || 'General';
}
