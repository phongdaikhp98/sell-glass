import api from './api'

export async function getProfile() {
  const res = await api.get('/v1/me')
  return res.data.data
}

export async function getAddresses() {
  const res = await api.get('/v1/me/addresses')
  return res.data.data
}

export async function addAddress(data: {
  receiverName: string
  phone: string
  address: string
  isDefault: boolean
}) {
  const res = await api.post('/v1/me/addresses', data)
  return res.data.data
}

export async function updateAddress(
  id: string,
  data: {
    receiverName: string
    phone: string
    address: string
    isDefault: boolean
  }
) {
  const res = await api.put(`/v1/me/addresses/${id}`, data)
  return res.data.data
}

export async function deleteAddress(id: string) {
  await api.delete(`/v1/me/addresses/${id}`)
}
