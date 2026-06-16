// Adding the specific API request types here for clean imports
export interface LoginRequest {
  email: string;
  password?: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password?: string;
  mobileNumber: string;
}
