package ru.profitsw2000.data.model.rcd

sealed class RcdInputPacketType {
    object TransmitterStateInputPacket: RcdInputPacketType()
    object ReceiverStateInputPacket: RcdInputPacketType()
    object SynthesizerStateInputPacket: RcdInputPacketType()
    object InvalidInputPacket: RcdInputPacketType()
}