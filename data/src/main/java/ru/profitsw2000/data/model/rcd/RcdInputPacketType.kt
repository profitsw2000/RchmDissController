package ru.profitsw2000.data.model.rcd

sealed class RcdInputPacketType {
    object TransmitterStateInputPacket: RcdInputPacketType()
    object ReceiverStateInputPacket: RcdInputPacketType()
    object SynthesizerStateInputPacket: RcdInputPacketType()
    object SynthesizerRefRegisterInputPacket: RcdInputPacketType()
    object SynthesizerIntRegisterInputPacket: RcdInputPacketType()
    object SynthesizerFracRegisterInputPacket: RcdInputPacketType()
    object SynthesizerModRegisterInputPacket: RcdInputPacketType()
    object SynthesizerCtr1RegisterInputPacket: RcdInputPacketType()
    object SynthesizerCtr2RegisterInputPacket: RcdInputPacketType()
    object SynthesizerCtr3RegisterInputPacket: RcdInputPacketType()
    object SynthesizerLfm1RegisterInputPacket: RcdInputPacketType()
    object SynthesizerLfm2RegisterInputPacket: RcdInputPacketType()
    object SynthesizerLfm3RegisterInputPacket: RcdInputPacketType()
    object SynthesizerPraRegisterInputPacket: RcdInputPacketType()
    object SynthesizerPrwRegisterInputPacket: RcdInputPacketType()
    object InvalidInputPacket: RcdInputPacketType()
}